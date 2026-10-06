/* Visningsgruppering af bevarede kildekoder; ingen klinisk status udledes. */
'use strict';
(function(root){
  const local='https://example.org/ejournal-sdk-fhir-demo/';
  const roles=[
    {key:'action',label:'Aktionsdiagnoser'},
    {key:'secondary',label:'Bidiagnoser'},
    {key:'referral',label:'Henvisningsdiagnoser'},
    {key:'other',label:'Andre / ukendt art'}
  ];
  const kinds=[
    {key:'condition',label:'Sygdomme og tilstande'},
    {key:'symptom',label:'Symptomer og fund'},
    {key:'contact',label:'Kontaktårsager'},
    {key:'unknown',label:'Uklassificerede koder'}
  ];
  // Brede ICD-10-kapitelområder for SKS-hovedgruppe D, ikke en kodevalidator.
  const chapters=[
    ['infection','Infektioner','A00','B99'],
    ['neoplasm','Svulster','C00','D48'],
    ['blood','Blod og immunsystem','D50','D89'],
    ['endocrine','Hormoner, ernæring og stofskifte','E00','E90'],
    ['mental','Psykiske lidelser','F00','F99'],
    ['nervous','Nervesystem','G00','G99'],
    ['eye','Øjne','H00','H59'],
    ['ear','Ører','H60','H95'],
    ['circulatory','Hjerte og kredsløb','I00','I99'],
    ['respiratory','Luftveje','J00','J99'],
    ['digestive','Fordøjelsessystem','K00','K93'],
    ['skin','Hud','L00','L99'],
    ['musculoskeletal','Muskler, led og bindevæv','M00','M99'],
    ['genitourinary','Nyrer, urinveje og kønsorganer','N00','N99'],
    ['pregnancy','Graviditet, fødsel og barsel','O00','O99'],
    ['perinatal','Tilstande omkring fødslen','P00','P96'],
    ['congenital','Medfødte tilstande','Q00','Q99'],
    ['symptom','Symptomer og fund','R00','R99','symptom'],
    ['injury','Skader og forgiftninger','S00','T98'],
    ['contact','Kontaktårsager og helbredsforhold','Z00','Z99','contact']
  ].map(([key,label,start,end,kind='condition'])=>({key,label,start,end,kind}));
  const unknown={key:'unknown',label:'Uklassificerede koder',kind:'unknown'};
  function extension(r,name){return (r.extension||[]).find(e=>e.url===local+'StructureDefinition/'+name);}
  function sourceDate(r){
    const value=extension(r,'source-period')?.valuePeriod?.start;
    return value && Number.isFinite(Date.parse(value))?value:null;
  }
  function role(r){
    const text=(r.category||[]).map(c=>c.text||'').join(' ').trim().toLocaleLowerCase('da');
    if(['aktionsdiagnose','aktiondiagnose'].includes(text))return 'action';
    if(text==='bidiagnose')return 'secondary';
    if(['henvisningsdiagnose','henvisningdiagnose'].includes(text))return 'referral';
    return 'other';
  }
  function coding(r){return r.code?.coding?.find(c=>c.code?.trim())||null;}
  function classify(c){
    if(c?.system!==local+'CodeSystem/source-sks')return unknown;
    const code=c.code.trim().toUpperCase();
    if(!/^D[A-Z][0-9]{2}[A-Z0-9]*$/.test(code))return unknown;
    const base=code.slice(1,4);
    return chapters.find(ch=>base>=ch.start&&base<=ch.end)||unknown;
  }
  function newestFirst(a,b){
    const difference=(sourceDate(b)?Date.parse(sourceDate(b)):-Infinity)-(sourceDate(a)?Date.parse(sourceDate(a)):-Infinity);
    return (Number.isNaN(difference)?0:difference)||String(a.id||'').localeCompare(String(b.id||''));
  }
  function group(resources){
    const groups=new Map();
    resources.filter(r=>r.resourceType==='Condition').forEach((r,index)=>{
      const c=coding(r);
      // Samme tekst alene dokumenterer ikke samme sygdom. Kodeløse poster forbliver separate.
      const key=c?JSON.stringify([c.system||'',c.code.trim()]):'uncoded:'+index;
      if(!groups.has(key))groups.set(key,{key,code:c?.code.trim()||'',classification:classify(c),occurrences:[]});
      groups.get(key).occurrences.push(r);
    });
    return [...groups.values()].map(g=>{
      g.occurrences.sort(newestFirst);
      g.latest=g.occurrences[0];
      g.lastIdentified=sourceDate(g.latest);
      g.actions=g.occurrences.filter(r=>role(r)==='action');
      g.lastAction=g.actions.length?sourceDate(g.actions[0]):null;
      g.roles=[...new Set(g.occurrences.map(role))];
      g.episodeCount=new Set(g.occurrences.map(r=>extension(r,'source-forloeb-key')?.valueString).filter(Boolean)).size;
      return g;
    }).sort((a,b)=>newestFirst(a.latest,b.latest)||a.key.localeCompare(b.key));
  }
  const api={roles,kinds,chapters,role,sourceDate,classify,group};
  if(typeof module!=='undefined'&&module.exports)module.exports=api;
  else root.DiagnosisOverview=api;
})(globalThis);
