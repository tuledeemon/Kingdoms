param(
    [Parameter(Mandatory=$true)][string]$ClientJar,
    [string]$OutputPath=(Join-Path $PSScriptRoot '..\src\main\resources\kingdoms-icon-sprites.json')
)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=[IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $ClientJar).Path)
try {
    function Read-Asset([string]$Path) {
        $entry=$archive.GetEntry($Path)
        if($null -eq $entry){return $null}
        $reader=[IO.StreamReader]::new($entry.Open())
        try {return ($reader.ReadToEnd() | ConvertFrom-Json -AsHashtable)} finally {$reader.Dispose()}
    }
    function Asset-Path([string]$Id,[string]$Folder,[string]$Suffix) {
        if(!$Id.Contains(':')){$Id='minecraft:'+$Id}
        $parts=$Id.Split(':',2)
        return 'assets/'+$parts[0]+'/'+$Folder+'/'+$parts[1]+$Suffix
    }
    function Item-Sprite([string]$Texture) {
        if(!$Texture -or $Texture.StartsWith('#')){return $null}
        if(!$Texture.Contains(':')){$Texture='minecraft:'+$Texture}
        if($Texture.Split(':',2)[1].StartsWith('item/') -and $null -ne $archive.GetEntry((Asset-Path $Texture 'textures' '.png'))){return @{atlas='minecraft:items';sprite=$Texture}}
        return $null
    }
    function Model-Sprite([string]$Model) {
        $textures=@{};$seen=[Collections.Generic.HashSet[string]]::new()
        while($Model -and $seen.Add($Model)){
            $data=Read-Asset (Asset-Path $Model 'models' '.json')
            if(!$data){break}
            if($data.textures){foreach($key in $data.textures.Keys){if(!$textures.ContainsKey($key)){$textures[$key]=$data.textures[$key]}}}
            $Model=$data.parent
        }
        # For layered items, prefer a complete base layer over a transparent overlay.
        foreach($key in @($textures.Keys | Where-Object {$_ -match '^layer\d+$'} | Sort-Object -Descending)){
            $texture=$textures[$key];$refs=[Collections.Generic.HashSet[string]]::new()
            while($texture -and $texture.StartsWith('#') -and $refs.Add($texture)){$texture=$textures[$texture.Substring(1)]}
            $sprite=Item-Sprite $texture
            if($sprite -and $texture -notmatch '(_overlay|_markings|_trim)$'){return $sprite}
        }
        return $null
    }
    function Definition-Sprite($Node) {
        if(!$Node){return $null}
        switch($Node.type){
            'minecraft:model' {return (Model-Sprite $Node.model)}
            'minecraft:condition' {return (Definition-Sprite $Node.on_false)}
            'minecraft:select' {
                if($Node.property -eq 'minecraft:display_context'){
                    foreach($case in $Node.cases){if(@($case.when) -contains 'gui'){$sprite=Definition-Sprite $case.model;if($sprite){return $sprite}}}
                }
                $sprite=Definition-Sprite $Node.fallback;if($sprite){return $sprite}
                foreach($case in $Node.cases){$sprite=Definition-Sprite $case.model;if($sprite){return $sprite}}
            }
            'minecraft:range_dispatch' {
                $sprite=Definition-Sprite $Node.fallback;if($sprite){return $sprite}
                foreach($entry in $Node.entries){$sprite=Definition-Sprite $entry.model;if($sprite){return $sprite}}
            }
            'minecraft:composite' {foreach($child in $Node.models){$sprite=Definition-Sprite $child;if($sprite){return $sprite}}}
        }
        return $null
    }
    $version=Read-Asset 'version.json'
    if($version.id -ne '26.2'){throw 'Use the exact Minecraft 26.2 client JAR'}
    $sprites=[ordered]@{}
    foreach($entry in @($archive.Entries | Where-Object {$_.FullName -match '^assets/minecraft/items/[^/]+\.json$'} | Sort-Object FullName)){
        $name=[IO.Path]::GetFileNameWithoutExtension($entry.FullName)
        $sprite=Item-Sprite ('minecraft:item/'+$name)
        if(!$sprite){$sprite=Definition-Sprite (Read-Asset $entry.FullName).model}
        if($sprite){$sprites['minecraft:'+$name]=$sprite}
    }
    # Shields are rendered in 3D and have no flat item sprite. Use vanilla's shield silhouette.
    $shieldPath='assets/minecraft/textures/gui/sprites/container/slot/shield.png'
    if(!$archive.GetEntry($shieldPath)){throw 'Missing shield GUI sprite'}
    $guiAtlas=Read-Asset 'assets/minecraft/atlases/gui.json'
    if(!@($guiAtlas.sources | Where-Object {$_.type -eq 'minecraft:directory' -and $_.source -eq 'gui/sprites' -and $_.prefix -eq ''}).Count){throw 'Shield sprite is not in the GUI atlas'}
    $sprites['minecraft:shield']=@{atlas='minecraft:gui';sprite='minecraft:container/slot/shield'}
    $result=[ordered]@{minecraft='26.2';sprites=$sprites}
    [IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath),($result | ConvertTo-Json -Depth 8)+"`n",[Text.UTF8Encoding]::new($false))
    Write-Output ('Generated '+$sprites.Count+' verified sprite mappings from Minecraft '+$version.id)
    foreach($name in @('clock','compass','recovery_compass','crossbow','debug_stick','enchanted_golden_apple','tipped_arrow','shield')){Write-Output ($name+' => '+($sprites['minecraft:'+$name] | ConvertTo-Json -Compress))}
} finally {$archive.Dispose()}
