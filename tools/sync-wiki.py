"""Generate the public GitHub wiki from the versioned Kingdoms guides."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
WIKI = ROOT / 'wiki'
WIKI.mkdir(exist_ok=True)

# Small caps are an in-game style. Web headings use ordinary searchable text.
HEADINGS = [
    'Getting started', 'Menus and territory', 'Claims and outposts',
    'Members, mayors and permissions', 'Land flags', 'Treasury and upkeep',
    'Wars', 'Kingdom beacon powers', 'Town centers and cores',
    'Mobs and animals', 'Identity and leaderboard', 'Command reference',
    'AirDrops', 'Artifacts and AirDrop cash',
]

def read(name):
    return (ROOT / name).read_text(encoding='utf-8')

def write(name, content):
    (WIKI / (name + '.md')).write_text(content.strip() + '\n', encoding='utf-8')

def slug(title):
    return re.sub(r'[^\w\- ]', '', title.lower()).replace(' ', '-')

def toc(content):
    return '\n'.join('- [' + title + '](#' + slug(title) + ')'
                     for title in re.findall(r'^## (.+)$', content, re.M))

def wiki_links(content):
    return (content.replace('(AIRDROPS_GUIDE.md)', '(AirDrops)')
                   .replace('(ARTIFACTS_GUIDE.md)', '(Artifacts)')
                   .replace('(PLAYER_GUIDE.md)', '(How-Kingdoms-Works)')
                   .replace('(PARACHUTE_MODEL.md)', '('
                       'https://github.com/tuledeemon/Kingdoms/blob/main/PARACHUTE_MODEL.md)'))

player = read('PLAYER_GUIDE.md')
player = re.sub(r'^# .+$', '# How Kingdoms Works', player, count=1, flags=re.M)
headings = iter(HEADINGS)
player = re.sub(r'^## .+$', lambda _: '## ' + next(headings), player, flags=re.M)
player = wiki_links(player)
intro, sections = player.split('\n## ', 1)
write('How-Kingdoms-Works', intro + '\n\n## Contents\n\n' + toc(player)
      + '\n\n## ' + sections)

airdrop = wiki_links(read('AIRDROPS_GUIDE.md'))
air_players, air_admin = airdrop.split('## Admin testing commands', 1)
write('AirDrops', air_players.replace('# AirDrops — Kingdoms 1.7.1', '# AirDrops')
      + '\n## More information\n\n[Admin commands and configuration](AirDrop-Administration).')
write('AirDrop-Administration', '# AirDrop Administration\n\n'
      'These commands require administrator access. Players should start with [AirDrops](AirDrops).\n\n'
      '## Admin testing commands' + air_admin)

artifact = wiki_links(read('ARTIFACTS_GUIDE.md'))
artifact_players, artifact_admin = artifact.split('## Configuration', 1)
write('Artifacts', artifact_players.replace('# Artifacts — Kingdoms 1.7.1', '# Artifacts')
      + '\n## More information\n\n[Configuration and admin commands](Artifact-Administration).')
write('Artifact-Administration', '# Artifact Administration\n\n## Configuration' + artifact_admin)

commands = player.split('## Command reference\n', 1)[1].split('\n## ', 1)[0]
write('Commands', '# Player Commands\n\n' + commands)

reference = read('README.md')
if (ROOT / 'PROJECT_REFERENCE.md').exists():
    reference = read('PROJECT_REFERENCE.md')
placeholders = reference.split('## Placeholders\n', 1)[1].split('\n## ', 1)[0]
write('Placeholders', '# Placeholders\n\n' + placeholders)
admin = reference.split('## Commands\n', 1)[1].split('\n## ', 1)[0]
customization = reference.split('## Customization\n', 1)[1].split('\n## ', 1)[0]
write('Administration', '# Kingdom Administration\n\n'
      'Defaults can be changed by server staff. Player guides describe stock Kingdoms settings, '
      'so always check the live menus for your server\'s prices and limits.\n\n'
      '## Commands\n\n' + admin + '\n\n## Configuration\n\n' + customization)
installation = reference.split('## Install\n', 1)[1].split('\n## ', 1)[0]
write('Installation', '# Installation\n\n' + installation + '\n\n'
      '## Building from source\n\nSee the repository\'s `BUILDING.md`. '
      'Kingdoms is a Fabric server mod; it does not run on Paper or Folia. '
      'The world border and Discord account linking belong to the server setup, not this mod.\n\n'
      '## Verification\n\nThe 1.7.1 release passed 102 unit tests, 545 dedicated-server '
      'checks, and 546 checks with C2ME. A connected Minecraft client and live anticheat '
      'compatibility have not been tested.')

write('Home', '''# Kingdoms Guide

Kingdoms is a server-side Minecraft mod for building kingdoms, claiming land and competing with other players. Join friends, manage a shared treasury, fight wars, and compete for AirDrops and special items. You can also stay independent and play solo.

**Start with `/kingdom`.** The menus handle most day-to-day tasks. Players need Minecraft Java 26.2, with no client mod or resource pack.

## Player guide

- **[How Kingdoms Works](How-Kingdoms-Works)** — the complete walkthrough, from your first claim to wars and captured cores.
- **[AirDrops](AirDrops)** — finding crates, capturing them and collecting rewards.
- **[Artifacts](Artifacts)** — the eight special items, their abilities, repairs and upgrades.
- **[Commands](Commands)** — a quick reference when you need a specific command.

## For server owners

- [Installation](Installation)
- [Kingdom administration](Administration)
- [AirDrop administration](AirDrop-Administration)
- [Artifact administration](Artifact-Administration)
- [Placeholders](Placeholders)

This guide covers Kingdoms 1.7.1. Prices, timers and limits describe default settings; server owners can change them. Check your server's menus and rules for its current settings.
''')
write('_Sidebar', '''**Kingdoms**

[Home](Home)

**Players**

- [How Kingdoms Works](How-Kingdoms-Works)
- [AirDrops](AirDrops)
- [Artifacts](Artifacts)
- [Commands](Commands)

**Server owners**

- [Installation](Installation)
- [Kingdom administration](Administration)
- [AirDrop administration](AirDrop-Administration)
- [Artifact administration](Artifact-Administration)
- [Placeholders](Placeholders)
''')
write('_Footer', 'Kingdoms 1.7.1 · Minecraft Java 26.2 · Default settings may differ from your server.')

pages = {p.stem for p in WIKI.glob('*.md')}
links = 0
for page in WIKI.glob('*.md'):
    for target in re.findall(r'\]\(([^)]+)\)', page.read_text(encoding='utf-8')):
        if target.startswith(('https://', 'http://', '#')):
            continue
        assert target.split('#')[0] in pages, (page.name, target)
        links += 1
print(f'Generated {len(pages)} wiki pages; verified {links} internal page links.')
