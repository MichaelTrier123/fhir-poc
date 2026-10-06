"""Build a dependency-free, offline SUP atlas from repository sources."""
from pathlib import Path
import xml.etree.ElementTree as ET
import json, re, hashlib, csv, unicodedata
from collections import Counter

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / 'docs/medcom/sup_documentation/Schema'
BACKGROUND = ROOT / 'docs/medcom/background_information'
OUT = ROOT / 'docs/medcom/modelatlas'
X = '{http://www.w3.org/2001/XMLSchema}'
PAGE_MARKER = r'<!--\s*(?:Kildeside:?\s*|Kilde:\s*PDF side\s*)(\d+)\s*-->'
def norm(s):
    s = s.lower().replace('æ','ae').replace('ø','oe').replace('å','aa')
    return re.sub('[^a-z0-9]', '', unicodedata.normalize('NFKD', s))
def doc(n):
    return ' '.join(' '.join(e.itertext()).strip() for e in n.findall(X+'annotation/'+X+'documentation'))

def build():
    OUT.mkdir(parents=True, exist_ok=True)
    pages, sections, sources = [], [], []
    for p in sorted(BACKGROUND.glob('*.md')):
        raw = p.read_text(encoding='utf-8-sig')
        sources.append({'file':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
        parts = re.split(PAGE_MARKER, raw)
        if len(parts)==1:
            pages.append({'id':f'{p.stem}:0','file':p.name,'page':0,'text':raw})
        for i in range(1,len(parts),2):
            pages.append({'id':f'{p.stem}:{parts[i]}','file':p.name,'page':int(parts[i]),'text':parts[i+1]})
        if 'domaenemodel' in p.stem:
            for m in re.finditer(r'^####\s+[\d.]+\s+(.+)\n([\s\S]*?)(?=^####|^###|^## |\Z)',raw,re.M):
                preceding = re.findall(PAGE_MARKER,raw[:m.start()])
                sections.append({'title':m[1].strip(),'text':m[2].strip(),'file':p.name,'page':int(preceding[-1]) if preceding else 1})
    def clinical(name, owner=''):
        matches = [s for s in sections if norm(s['title'])==norm(name)]
        if not matches and owner:
            matches = [s for s in sections if norm(s['title'])==norm(owner)]
        matches.sort(key=lambda s:('v2_2' not in s['file'],s['file']))
        refs=[]
        for s in matches[:2]:
            text=s['text']
            if owner:
                lines=text.splitlines(); start=None
                for i,line in enumerate(lines):
                    first=re.split(r'\s{2,}',line.strip())[0]
                    if norm(first)==norm(name): start=i; break
                if start is None: continue
                end=start+1
                while end<len(lines) and (not lines[end].strip() or lines[end].startswith('  ') or lines[end].startswith('<!--')): end+=1
                text='\n'.join(lines[start:end]).strip()
            refs.append({'title':s['title'],'file':s['file'],'page':s['page'],'text':text})
        return refs
    trees={p.name:ET.parse(p).getroot() for p in sorted(SCHEMA.glob('*.xsd'))}
    records=[]; schema_sources=[]; problems=[]
    def facets(n):
        found=[]
        def visit(e):
            if e is not n and e.tag in (X+'element',X+'attribute'):return
            if e.tag in [X+t for t in ('enumeration','pattern','minLength','maxLength','minInclusive','maxInclusive','union','restriction')]:found.append({'kind':e.tag.removeprefix(X),'value':e.get('value',e.get('memberTypes',e.get('base','')))})
            for c in e:visit(c)
        visit(n)
        return found
    def walk(n,path,file,owner,particles=()):
        for i,c in enumerate(n):
            tag=c.tag.removeprefix(X)
            cp=f'{path}/{tag}[{i+1}]'
            if tag in ('sequence','choice','all'):
                walk(c,cp,file,owner,particles+({'kind':tag,'min':c.get('minOccurs','1'),'max':c.get('maxOccurs','1'),'path':cp},))
            elif tag in ('element','attribute','attributeGroup','group','complexType','simpleType'):
                name=c.get('name',c.get('ref',''))
                rid=f'{file}:{cp}'
                global_=n.tag==X+'schema'
                if name:
                    records.append({'id':rid,'kind':tag,'name':name,'ref':c.get('ref'),'global':global_,'owner':owner,'file':file,'path':cp,'type':c.get('type','inline' if len(c) else ''),'min':c.get('minOccurs','1'),'max':c.get('maxOccurs','1'),'use':c.get('use','optional'),'particles':list(particles),'annotation':doc(c),'facets':facets(c),'clinical':clinical(name,owner if tag=='attribute' else ''),'xml':ET.tostring(c,encoding='unicode'),'default':c.get('default'),'fixed':c.get('fixed')})
                walk(c,cp,file,rid if name else owner,particles)
            else: walk(c,cp,file,owner,particles)
    for file,t in trees.items():
        p=SCHEMA/file
        schema_sources.append({'file':file,'namespace':t.get('targetNamespace'),'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'includes':[n.get('schemaLocation') for n in t.findall(X+'include')]})
        walk(t,'schema',file,'')
    byid={r['id']:r for r in records}
    # Attribute evidence uses its containing declaration's clinical class name.
    for r in records:
        if r['kind']=='attribute' and r['owner'] in byid:
            r['clinical']=clinical(r['name'],byid[r['owner']]['name'])
    globals_={}
    for r in records:
        if r['global'] and not r['ref']: globals_.setdefault((r['kind'],r['name']),[]).append(r)
    def closure(file, seen=None):
        seen=set() if seen is None else seen
        if file in seen:return seen
        seen.add(file)
        for inc in trees[file].findall(X+'include'):
            name=inc.get('schemaLocation')
            actual=next((f for f in trees if f.lower()==name.lower()),None)
            if actual:closure(actual,seen)
        return seen
    for (kind,name),variants in globals_.items():
        if len(variants)>1:
            problems.append({'kind':'variants','message':f'{kind} {name}: {len(variants)} globale deklarationer i forskellige service-/skema-kontekster; ikke én fælles definition.'})
            for r in variants:r['variants']=[v['id'] for v in variants if v is not r]
    for r in records:
        if r['ref']:
            candidates=globals_.get((r['kind'],r['ref'].split(':')[-1]),[])
            candidates=[v for v in candidates if v['file'] in closure(r['file'])]
            own=[v for v in candidates if v['file']==r['file']]
            candidates=own or candidates
            r['targets']=[v['id'] for v in candidates]
            r['target']=candidates[0]['id'] if len(candidates)==1 else None
            if not candidates: problems.append({'kind':'unresolved','id':r['id'],'message':f"Uopløst {r['kind']}-reference: {r['ref']}"})
            if len(candidates)>1:problems.append({'kind':'ambiguous','id':r['id'],'message':f"Flere mulige definitioner for {r['ref']} i include-konteksten"})
        if r['type'] not in ('','inline') and ':' not in r['type']:
            targets=globals_.get(('simpleType',r['type'])) or globals_.get(('complexType',r['type']),[])
            targets=[v for v in targets if v['file'] in closure(r['file'])]
            r['typeTarget']=targets[0]['id'] if len(targets)==1 else None
    for s in schema_sources:
        for inc in s['includes']:
            if inc not in trees:
                actual=next((f for f in trees if f.lower()==inc.lower()),None)
                problems.append({'kind':'include','message':f"{s['file']}: include {inc} har forkert bogstavstørrelse (fil: {actual}); virker på Windows, men kan fejle på case-sensitive filsystemer." if actual else f"{s['file']}: manglende include {inc}"})
    elements=[r for r in records if r['kind']=='element' and not r['ref']]
    attrs=[r for r in records if r['kind']=='attribute' and not r['ref']]
    usage=Counter(r.get('target') for r in records if r.get('target'))
    for r in records:r['reuse']=usage[r['id']]
    stats={'schemaFiles':len(trees),'globalElements':sum(r['global'] for r in elements),'localElements':sum(not r['global'] for r in elements),'elementDeclarations':len(elements),'elementNames':len({r['name'] for r in elements}),'elementReferences':sum(r['kind']=='element' and bool(r['ref']) for r in records),'attributeDeclarations':len(attrs),'attributeNames':len({r['name'] for r in attrs}),'attributeGroups':sum(r['global'] and r['kind']=='attributeGroup' for r in records),'reusedElements':sum(r['global'] and r['reuse']>1 for r in elements),'clinicalElements':sum(bool(r['clinical'] or r['annotation']) for r in elements),'clinicalAttributes':sum(bool(r['clinical'] or r['annotation']) for r in attrs),'issues':len(problems)}
    stats['uniqueGlobalElementNames']=len({r['name'] for r in elements if r['global']})
    stats['globalElementVariantNames']=sum(kind=='element' and len(v)>1 for (kind,name),v in globals_.items())
    data={'stats':stats,'records':records,'pages':pages,'sources':sources,'schemas':schema_sources,'issues':problems}
    (OUT/'atlas.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
    for name,rows in [('elements',elements),('attributes',attrs)]:
        with (OUT/f'{name}.csv').open('w',encoding='utf-8-sig',newline='') as f:
            fields=['id','name','global','owner','file','type','min','max','use','reuse','annotation']
            w=csv.DictWriter(f,fields,extrasaction='ignore');w.writeheader();w.writerows(rows)
    template=(ROOT/'scripts/sup_atlas_template.html').read_text(encoding='utf-8')
    (OUT/'index.html').write_text(template.replace('__ATLAS_DATA__',json.dumps(data,ensure_ascii=False).replace('</','<\\/')),encoding='utf-8')
    (OUT/'summary.json').write_text(json.dumps(stats,indent=2),encoding='utf-8')
    print(json.dumps(stats,indent=2))
    assert len({r['id'] for r in records})==len(records)
    assert all(r['owner'] in byid for r in records if r['owner'])
    assert sum(usage.values())==sum(bool(r.get('target')) for r in records)
    assert {s['file'] for s in sources}=={p['file'] for p in pages}
    assert all(any(p['file']==s['file'] and p['page']==s['page'] for p in pages) for r in records for s in r['clinical'])
    return data
if __name__=='__main__':build()
