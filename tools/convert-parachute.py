"""Convert the CC0 GLB parachute into bounded, rotated vanilla block displays.

The source's canopy rings and rigging angles are decoded from the GLB. Unlike
voxelization, thin rotated fabric strips preserve slopes without a stepped dome.
Requires Python and numpy; Pillow is only used for the optional geometry preview.
"""
from pathlib import Path
import argparse, hashlib, json, math, struct
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
SOURCE_URL = 'https://cdn.3dassets.dev/assets/27454/v1/model.glb'
SOURCE_PAGE = 'https://3dassets.dev/assets/wipe-day-survival-island-airdrop-crate-ef2e0e2f'
CONVERTER = 'https://github.com/SansCraft-Network/sanscraft-bde#3d-model-converters'

def mesh_components(path):
    raw = path.read_bytes()
    magic, version, length = struct.unpack_from('<III', raw)
    if magic != 0x46546c67 or version != 2 or length != len(raw):
        raise ValueError('Invalid GLB 2.0 model')
    offset, scene, binary = 12, None, None
    while offset < len(raw):
        size, kind = struct.unpack_from('<II', raw, offset)
        chunk = raw[offset+8:offset+8+size]
        if kind == 0x4e4f534a: scene = json.loads(chunk)
        if kind == 0x004e4942: binary = chunk
        offset += 8+size
    def accessor(index):
        a = scene['accessors'][index]; v = scene['bufferViews'][a['bufferView']]
        fmt, bits, signed = {5120:('b',8,True),5121:('B',8,False),5122:('h',16,True),5123:('H',16,False),5125:('I',32,False),5126:('f',32,False)}[a['componentType']]
        width = {'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']]
        step = v.get('byteStride',struct.calcsize(fmt)*width); start = v.get('byteOffset',0)+a.get('byteOffset',0)
        values = np.array([struct.unpack_from('<'+fmt*width,binary,start+i*step) for i in range(a['count'])],dtype=float)
        if a.get('normalized') and fmt != 'f':
            values /= 2**(bits-1)-1 if signed else 2**bits-1
            if signed: values = np.maximum(values,-1)
        return values
    components=[]
    def walk(index,parent):
        node=scene['nodes'][index]
        if 'matrix' in node: matrix=np.array(node['matrix']).reshape((4,4),order='F')
        else:
            x,y,z,w=node.get('rotation',[0,0,0,1])
            matrix=np.array([[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w),0],[2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w),0],[2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y),0],[0,0,0,1]],dtype=float)
            matrix[:3,:3] @= np.diag(node.get('scale',[1,1,1]));matrix[:3,3]=node.get('translation',[0,0,0])
        transform=parent @ matrix
        if 'mesh' in node:
            for primitive in scene['meshes'][node['mesh']]['primitives']:
                if primitive.get('mode',4)!=4: raise ValueError('Only triangle primitives supported')
                vertices=accessor(primitive['attributes']['POSITION'])
                vertices=(transform @ np.c_[vertices,np.ones(len(vertices))].T).T[:,:3]
                indices=accessor(primitive['indices']).ravel().astype(int) if 'indices' in primitive else np.arange(len(vertices))
                triangles=vertices[indices].reshape((-1,3,3))
                # Group separate shells/ropes even though the model shares a material.
                parents={tuple(np.round(p,4)):tuple(np.round(p,4)) for p in vertices}
                def find(p):
                    if parents[p]!=p: parents[p]=find(parents[p])
                    return parents[p]
                for triangle in triangles:
                    points=[tuple(np.round(p,4)) for p in triangle]
                    for p in points[1:]:parents[find(p)]=find(points[0])
                groups={}
                for p in parents:groups.setdefault(find(p),set()).add(p)
                material=scene['materials'][primitive['material']]['name']
                components.extend({'material':material,'points':np.array(list(points))} for points in groups.values())
        for child in node.get('children',[]):walk(child,transform)
    for node in scene['scenes'][scene.get('scene',0)]['nodes']:walk(node,np.eye(4))
    return components

def unit(v): return v/np.linalg.norm(v)

def quaternion(matrix):
    # Orthonormal, right-handed basis to Minecraft's x/y/z/w quaternion.
    m=matrix; trace=np.trace(m)
    if trace>0:
        s=math.sqrt(trace+1)*2;q=[(m[2,1]-m[1,2])/s,(m[0,2]-m[2,0])/s,(m[1,0]-m[0,1])/s,s/4]
    else:
        i=int(np.argmax(np.diag(m)));j=(i+1)%3;k=(i+2)%3;s=math.sqrt(1+m[i,i]-m[j,j]-m[k,k])*2
        q=[0.,0.,0.,0.];q[i]=s/4;q[j]=(m[j,i]+m[i,j])/s;q[k]=(m[k,i]+m[i,k])/s;q[3]=(m[k,j]-m[j,k])/s
    return (np.array(q)/np.linalg.norm(q)).tolist()

def convert(path):
    components=mesh_components(path)
    canopy=next(c for c in components if c['material']=='cloth' and len(c['points'])==26 and c['points'][:,1].min()>2)
    p=canopy['points'];rim_y=float(p[np.argmax(np.linalg.norm(p[:,[0,2]],axis=1)),1]);top=float(p[:,1].max())
    shoulder=max(y for y in sorted(set(p[:,1])) if rim_y+.05<y<top-.05)
    shoulder_radius=float(np.linalg.norm(p[np.isclose(p[:,1],shoulder,atol=.001)][0][[0,2]]))
    radius=float(np.linalg.norm(p[:,[0,2]],axis=1).max());factor=2.7/radius
    def height(y):return 5+(y-rim_y)*factor
    rings=[(.14,top),(shoulder_radius,shoulder),(radius,rim_y)]
    parts=[]
    def box(role,center,basis,size):
        assert np.linalg.det(basis)>.999 and min(size)>0
        offset=center-basis @ (np.array(size)/2)
        parts.append({'role':role,'offset':[round(float(x),6) for x in offset],'scale':[round(float(x),6) for x in size],'rotation':[round(x,8) for x in quaternion(basis)]})
    def point(radius,y,angle):return np.array([.5+radius*factor*math.cos(angle),height(y),.5+radius*factor*math.sin(angle)])
    def rod(role,a,b,width):
        y=unit(b-a);reference=np.array([0,0,1.]) if abs(y[2])<.9 else np.array([1.,0,0])
        x=unit(np.cross(y,reference));z=np.cross(x,y);box(role,(a+b)/2,np.column_stack([x,y,z]),[width,np.linalg.norm(b-a),width])
    for sector in range(16):
        a0=sector*math.tau/16;a1=(sector+1)*math.tau/16;mid=(a0+a1)/2
        role='primary' if (sector//2)%2==0 else 'secondary'
        # Slice tapered gores into fitted thin fabric panels; a small overlap avoids seams opening.
        for (r0,y0),(r1,y1) in zip(rings,rings[1:]):
            for band in range(3):
                t0=band/3;t1=(band+1)/3;t=(t0+t1)/2
                ra=r0+(r1-r0)*t0;rb=r0+(r1-r0)*t1;ya=y0+(y1-y0)*t0;yb=y0+(y1-y0)*t1
                ca=(point(ra,ya,a0)+point(ra,ya,a1))/2;cb=(point(rb,yb,a0)+point(rb,yb,a1))/2
                u=unit(point((ra+rb)/2,(ya+yb)/2,a1)-point((ra+rb)/2,(ya+yb)/2,a0));v=unit(cb-ca);n=np.cross(v,u)
                width=2*rb*factor*math.sin(math.pi/16)+.025
                box(role,(ca+cb)/2,np.column_stack([u,n,v]),[width,.04,np.linalg.norm(cb-ca)+.025])
        # Shallow inner lining rather than the source's solid lower half.
        for band in range(2):
            ra=.14+(radius-.14)*band/2;rb=.14+(radius-.14)*(band+1)/2
            ca=(point(ra,rim_y-.035,a0)+point(ra,rim_y-.035,a1))/2;cb=(point(rb,rim_y-.035,a0)+point(rb,rim_y-.035,a1))/2
            u=unit(point((ra+rb)/2,rim_y-.035,a1)-point((ra+rb)/2,rim_y-.035,a0));v=unit(cb-ca);n=np.cross(v,u)
            box('lining',(ca+cb)/2,np.column_stack([u,n,v]),[2*rb*factor*math.sin(math.pi/16)+.025,.025,np.linalg.norm(cb-ca)+.025])
        rod('trim',point(radius,rim_y,a0),point(radius,rim_y,a1),.045)
        rod('trim',point(.14,top,a0),point(.14,top,a1),.04)
        if sector%2==0:
            for (r0,y0),(r1,y1) in zip(rings,rings[1:]):rod('seam',point(r0,y0+.016,a0),point(r1,y1+.016,a0),.018)
    ropes=sorted((c for c in components if c['material']=='cloth' and len(c['points'])==10),key=lambda c:math.atan2(c['points'][:,2].mean(),c['points'][:,0].mean()))
    if len(ropes)!=8:raise ValueError('Expected eight source suspension lines')
    for rope in ropes:
        direction=rope['points'][:,[0,2]].mean(axis=0);angle=math.atan2(direction[1],direction[0])
        a=np.array([.5+.42*math.cos(angle),1.02,.5+.42*math.sin(angle)]);b=point(radius,rim_y,angle)
        rod('rope',a,b,.032);box('trim',b,np.eye(3),[.08,.08,.08])
    return {'schemaVersion':1,'source':SOURCE_PAGE,'license':'CC0-1.0','sourceSha256':hashlib.sha256(path.read_bytes()).hexdigest(),'conversion':'GLB canopy/rigging extraction; fitted rotated fabric strips; sixteen curved gores; recolored vanilla wool','voxelConverterReference':CONVERTER,'parts':parts}

def preview(model,path):
    from PIL import Image,ImageDraw,ImageFont
    image=Image.new('RGB',(1100,1000),'#101925');draw=ImageDraw.Draw(image)
    palette={'primary':(227,112,37),'secondary':(230,222,202),'lining':(181,176,160),'seam':(150,139,116),'trim':(85,74,57),'rope':(234,226,204),'crate':(110,76,43)}
    faces=[];eye=np.array([8.,7.,11.]);target=np.array([.5,3.2,.5]);forward=unit(target-eye);right=unit(np.cross(forward,[0,1.,0]));up=np.cross(right,forward)
    vertices=np.array([[x,y,z] for x in [0,1] for y in [0,1] for z in [0,1]],dtype=float)
    indices=[[0,1,3,2],[4,6,7,5],[0,4,5,1],[2,3,7,6],[0,2,6,4],[1,5,7,3]]
    def rotation(q):
        x,y,z,w=q;return np.array([[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w)],[2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w)],[2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y)]])
    parts=model['parts']+[{'role':'crate','offset':[0,0,0],'scale':[1,1,1],'rotation':[0,0,0,1]}]
    for part in parts:
        points=(rotation(part['rotation']) @ (vertices*np.array(part['scale'])).T).T+part['offset']
        for face in indices:
            polygon=points[face];center=polygon.mean(axis=0);normal=unit(np.cross(polygon[1]-polygon[0],polygon[2]-polygon[0]));shade=.72+.28*abs(np.dot(normal,unit(np.array([-.4,1,.3]))))
            coords=[]
            for p in polygon:
                delta=p-eye;depth=np.dot(delta,forward);coords.append((550+np.dot(delta,right)*1230/depth,505-np.dot(delta,up)*1230/depth))
            color=tuple(round(v*shade) for v in palette[part['role']]);faces.append((np.dot(center-eye,forward),coords,color))
    for depth,coords,color in sorted(faces,reverse=True,key=lambda f:f[0]):draw.polygon(coords,fill=color)
    draw.text((32,28),'KINGDOMS / AIRDROP PARACHUTE',fill='#eee1c5',font=ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf',26))
    draw.text((32,945),'Geometry preview - vanilla wool textures are applied in Minecraft',fill='#b4c2d1',font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',18))
    path.parent.mkdir(parents=True,exist_ok=True);image.save(path)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,default=ROOT/'research/parachute/airdrop-source.glb');parser.add_argument('--output',type=Path,default=ROOT/'src/main/resources/data/kingdoms/airdrop/parachute.json');parser.add_argument('--preview',type=Path,default=ROOT/'research/parachute/parachute-preview.png');args=parser.parse_args()
    model=convert(args.source);assert len(model['parts'])<=256
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8');preview(model,args.preview)
    print(json.dumps({'displays':len(model['parts']),'roles':{role:sum(p['role']==role for p in model['parts']) for role in set(p['role'] for p in model['parts'])},'model':str(args.output),'preview':str(args.preview)},indent=2))
