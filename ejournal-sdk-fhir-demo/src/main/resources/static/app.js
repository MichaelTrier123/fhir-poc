/* Patientdata holdes kun i hukommelsen. Al kildetekst indsættes med textContent. */
'use strict';
(() => {
const layoutVersion='episode-journal-5';
const requiredElements=[
  'patient-form','cpr','load','feedback','journal','empty-state','inspector','json','inspector-title',
  'search','detail-search','diagnosis-search','diagnosis-chapter','diagnosis-view','journal-view',
  'close-inspector','download','episodes','episode-shortcuts','detail-eyebrow','detail-title',
  'detail-subtitle','episode-json','episode-context','detail-search-control','filters','resources',
  'detail-panel-body','diagnosis-panel','journal-panel','diagnosis-stats','diagnosis-roles',
  'diagnosis-result-count','diagnosis-groups','patient-name','initials','patient-cpr',
  'resource-total','episode-count','quality','quality-summary','warnings',
  'reader','reader-title','reader-content','close-reader','journal-workspace','episode-years','return-overview'
];
if(document.documentElement.dataset.journalLayout!==layoutVersion||requiredElements.some(id=>!document.getElementById(id))){
  const feedback=document.getElementById('feedback');
  if(feedback){feedback.className='feedback error';feedback.textContent='Journalens side og programfiler passer ikke sammen. Genindlæs siden helt (Ctrl+F5 eller Cmd+Shift+R), så den aktuelle version hentes.';}
  const load=document.getElementById('load');if(load)load.disabled=true;
  return;
}
const $ = id => document.getElementById(id);
const local = 'https://example.org/ejournal-sdk-fhir-demo/StructureDefinition/';
let state = null;
let selected = 'all';
let filter = 'summary';
let diagnosisRole = 'all';
let diagnosisKind = 'all';
let episodeYearGroups=[];
const formatter = new Intl.DateTimeFormat('da-DK',{dateStyle:'medium',timeStyle:'short',timeZone:'Europe/Copenhagen'});
function node(tag,className,text){const n=document.createElement(tag);if(className)n.className=className;if(text!==undefined)n.textContent=text;return n;}
function ext(r,name){return (r.extension||[]).find(e=>e.url===local+name);}
function period(r){return r.period || ext(r,'source-period')?.valuePeriod || {};}
function date(value){if(!value)return 'Dato ikke oplyst';const d=new Date(value);return Number.isNaN(d.getTime())?'Ugyldig kildedato':formatter.format(d);}
function range(r){const p=period(r);return date(p.start)+(p.end?' – '+date(p.end):'');}
function sourceType(r){return ext(r,'source-type')?.valueString||'';}
function episodeKey(r){return ext(r,'source-forloeb-key')?.valueString;}
function episodeUpdated(r){return ext(r,'source-updated')?.valueDateTime;}
function isCave(r){return r.resourceType==='DocumentReference' && sourceType(r)==='CaveOplysninger';}
function organization(r){
  const reference=r.managingOrganization||r.serviceProvider||ext(r,'source-organization')?.valueReference;
  const org=state?.byUrl.get(reference?.reference);
  const parent=state?.byUrl.get(org?.partOf?.reference);
  return [org?.name,parent?.name].filter(Boolean).join(' · ');
}
function title(r){
  if(r.resourceType==='EpisodeOfCare')return organization(r)||'Forløb uden afdelingsnavn';
  if(r.resourceType==='Encounter')return ({AMB:'Ambulant kontakt',IMP:'Indlæggelseskontakt',UNK:'Kontakt · ukendt type'})[r.class?.code]||'Kontakt';
  return r.code?.text||r.description||r.code?.coding?.[0]?.code||r.resourceType;
}
function searchText(r){return [title(r),organization(r),r.code?.coding?.map(c=>c.code).join(' '),(Array.isArray(r.category)?r.category:[r.category]).filter(Boolean).map(c=>c.text).join(' '),sourceType(r),documentText(r)].join(' ').toLocaleLowerCase('da');}
function documentText(r){
  const data=r.content?.[0]?.attachment?.data;if(!data)return '';
  try{return new TextDecoder().decode(Uint8Array.from(atob(data),c=>c.charCodeAt(0)));}catch{return 'Dokumentteksten kunne ikke læses.';}
}
function inspect(r){$('inspector-title').textContent=r.resourceType;$('json').textContent=JSON.stringify(r,null,2);$('inspector').showModal();}
function action(label,handler,className='text-button'){const b=node('button',className,label);b.type='button';b.addEventListener('click',handler);return b;}
function icon(key){
  const paths={
    episode:['M3 6h6l2 2h10v12H3z','M3 6V4h6l2 2'],
    calendar:['M4 5h16v15H4z','M8 3v4M16 3v4M4 10h16'],
    Condition:['M8 3h8v5h5v8h-5v5H8v-5H3V8h5z'],
    Encounter:['M16 6a4 4 0 1 1-8 0a4 4 0 0 1 8 0','M4 21v-2a8 8 0 0 1 16 0v2'],
    Procedure:['M2 12h5l3-8 4 16 3-8h5'],
    DocumentReference:['M5 3h10l4 4v14H5z','M14 3v5h5M8 12h8M8 16h6'],
    Notater:['M5 3h10l4 4v14H5z','M8 12h8M8 16h5'],
    Epikriser:['M5 3h10l4 4v14H5z','M8 11h8M8 15h5M14 19h7M18 16l3 3-3 3'],
    referral:['M4 12h16M14 6l6 6-6 6'],
    timeline:['M5 3v18M5 6h14M5 12h9M5 18h14']
  };
  const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');
  for(const [name,value] of Object.entries({viewBox:'0 0 24 24',fill:'none',stroke:'currentColor','stroke-width':'1.6','stroke-linecap':'round','stroke-linejoin':'round','aria-hidden':'true',focusable:'false',class:'ui-icon'}))svg.setAttribute(name,value);
  for(const d of paths[key]||paths.episode){const path=document.createElementNS(svg.namespaceURI,'path');path.setAttribute('d',d);svg.append(path);}return svg;
}
function clinical(){return state.resources.filter(r=>['Encounter','Condition','Procedure','DocumentReference'].includes(r.resourceType));}
const journalTabs=[
  {key:'summary',label:'Sammenfatning'}, {key:'timeline',label:'Tidslinje'},
  {key:'Condition',label:'Diagnoser'}, {key:'Encounter',label:'Kontaktperioder'},
  {key:'Procedure',label:'Procedurer'}, {key:'documents',label:'Dokumenter'}
];
const dayFormatter=new Intl.DateTimeFormat('da-DK',{dateStyle:'medium',timeZone:'Europe/Copenhagen'});
function shortDate(value){return timestamp(value)===null?'Dato ikke oplyst':dayFormatter.format(new Date(value));}
function openEpisode(key){$('reader').close();$('search').value='';setSelection(key);renderEpisodes();showView('journal');$('detail-title').focus({preventScroll:true});}
function readResources(label,resources){
  $('reader-title').textContent=label;
  $('reader-content').replaceChildren(...resources.map(r=>{
    const card=resourceCard(r),episode=state.episodes.find(e=>episodeKey(e)===episodeKey(r));
    if(r.resourceType==='DocumentReference')card.querySelector('details').open=true;
    if(episode)card.append(action('Se forløb →',()=>openEpisode(episodeKey(r))));
    return card;
  }));
  $('reader').showModal();
}
function timestamp(value){const time=Date.parse(value||'');return Number.isFinite(time)?time:null;}
function latestUpdatedFirst(a,b){
  const left=timestamp(episodeUpdated(a)),right=timestamp(episodeUpdated(b));
  if(left===null||right===null){if(left!==right)return left===null?1:-1;}
  else if(left!==right)return right-left;
  const startLeft=timestamp(period(a).start),startRight=timestamp(period(b).start);
  if(startLeft===null||startRight===null){if(startLeft!==startRight)return startLeft===null?1:-1;}
  else if(startLeft!==startRight)return startRight-startLeft;
  return String(a.id||'').localeCompare(String(b.id||''));
}
const episodeContentTypes=[
  {key:'Condition',label:'Diagnoser',matches:r=>r.resourceType==='Condition'},
  {key:'Encounter',label:'Kontakter',matches:r=>r.resourceType==='Encounter'},
  {key:'Procedure',label:'Procedurer',matches:r=>r.resourceType==='Procedure'},
  {key:'Notater',label:'Notater',matches:r=>r.resourceType==='DocumentReference'&&sourceType(r)==='Notater'},
  {key:'Epikriser',label:'Epikriser',matches:r=>r.resourceType==='DocumentReference'&&sourceType(r)==='Epikriser'}
];
function chronological(a,b){
  const left=timestamp(period(a).start),right=timestamp(period(b).start);
  if(left===null&&right!==null)return 1;if(right===null&&left!==null)return -1;
  return (left===null?0:left-right)||String(a.id||'').localeCompare(String(b.id||''));
}
function scoped(){return selected==='cave'?clinical().filter(isCave):state.profiles.get(selected)?.items||[];}
function buildEpisodeProfiles(){
  const itemsByEpisode=new Map();
  for(const r of clinical()){
    const key=episodeKey(r);if(!key||isCave(r))continue;
    if(!itemsByEpisode.has(key))itemsByEpisode.set(key,[]);itemsByEpisode.get(key).push(r);
  }
  return new Map(state.episodes.map(episode=>{
    const key=episodeKey(episode),items=itemsByEpisode.get(key)||[];
    const diagnoses=items.filter(r=>r.resourceType==='Condition');
    const byRole=new Map(DiagnosisOverview.roles.map(role=>[role.key,DiagnosisOverview.group(diagnoses.filter(r=>DiagnosisOverview.role(r)===role.key))]));
    const actions=byRole.get('action');
    const lastAction=actions.find(g=>g.lastIdentified)?.lastIdentified;
    const latestActions=lastAction?actions.filter(g=>timestamp(g.lastIdentified)===timestamp(lastAction)):[];
    const referrals=diagnoses.filter(r=>DiagnosisOverview.role(r)==='referral').sort(chronological);
    const counts=episodeContentTypes.map(type=>({...type,count:items.filter(type.matches).length}));
    return [key,{key,episode,items,counts,diagnoses:DiagnosisOverview.group(diagnoses),byRole,latestActions,lastAction,
      undatedActions:diagnoses.some(r=>DiagnosisOverview.role(r)==='action'&&!DiagnosisOverview.sourceDate(r)),
      firstReferral:referrals.find(r=>DiagnosisOverview.sourceDate(r)),overview:ext(episode,'source-overview-diagnosis')?.valueCodeableConcept}];
  }));
}
function conceptTitle(concept){return concept?.text||concept?.coding?.[0]?.code||'Diagnose ikke oplyst';}
function episodeDiagnosis(profile){
  if(profile.latestActions.length)return profile.latestActions.map(g=>title(g.latest)).join(' · ');
  if(profile.byRole.get('action').length)return profile.byRole.get('action').map(g=>title(g.latest)).join(' · ');
  return profile.overview?conceptTitle(profile.overview):'Aktionsdiagnose ikke oplyst';
}
function episodeDiagnosisLabel(profile){
  return profile.latestActions.length?(profile.undatedActions?'Senest daterede aktionsdiagnose':'Seneste aktionsdiagnose')+(profile.latestActions.length>1?'r':''):
    profile.byRole.get('action').length?'Aktionsdiagnoser · dato ikke oplyst':profile.overview?'Forløbsdiagnose fra oversigten':'Diagnosegrundlag';
}
function setSelection(value){
  selected=value;filter=value==='cave'?'CAVE':'summary';$('detail-search').value='';
  updateJournalLayout();
  document.querySelectorAll('[data-episode-selection]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.episodeSelection===selected)));
  document.querySelectorAll('[data-episode-row]').forEach(row=>row.classList.toggle('is-selected',row.dataset.episodeRow===selected));
  renderDetails();$('detail-panel-body').scrollTop=0;
}
function setJournalTab(value){filter=value;renderDetails();$('detail-panel-body').scrollTop=0;document.querySelector('#filters button[aria-pressed="true"]')?.focus({preventScroll:true});}
function updateJournalLayout(){
  $('journal-workspace').classList.toggle('is-detail',selected!=='all');$('return-overview').hidden=selected==='all';
}
function render(){updateJournalLayout();renderEpisodes();renderDetails();}
function selectionButton(label,value,className){
  const b=action(label,()=>setSelection(value),className);b.dataset.episodeSelection=value;b.setAttribute('aria-pressed',String(selected===value));return b;
}
function compactDate(value){
  const time=timestamp(value);return time===null?'Dato ukendt':new Intl.DateTimeFormat('da-DK',{day:'2-digit',month:'2-digit',year:'2-digit',timeZone:'Europe/Copenhagen'}).format(new Date(time));
}
const compactMomentFormatter=new Intl.DateTimeFormat('da-DK',{dateStyle:'short',timeStyle:'short',timeZone:'Europe/Copenhagen'});
function compactMoment(value){const time=timestamp(value);return time===null?'Ikke oplyst':compactMomentFormatter.format(new Date(time));}
function episodeNavigationRow(profile){
  const row=node('tr','episode-nav-row'),org=organization(profile.episode),p=period(profile.episode);
  row.dataset.episodeRow=profile.key;row.classList.toggle('is-selected',selected===profile.key);
  const updated=episodeUpdated(profile.episode),dates=node('td','episode-nav-period');dates.title=range(profile.episode)+' · Senest opdateret: '+date(updated);
  dates.append(node('span','episode-nav-date-label','Opdateret'),node('span','episode-nav-updated',compactMoment(updated)),node('span','episode-nav-date-label','Start'),node('span','episode-nav-start',compactMoment(p.start)));
  const cell=node('td','episode-nav-description'),b=selectionButton('',profile.key,'episode-row-button');
  const label=profile.latestActions.length?(profile.undatedActions?'Senest daterede aktion':'Seneste aktion'):profile.byRole.get('action').length?'Aktion · dato ukendt':profile.overview?'Diagnose fra oversigten':'Diagnose ikke oplyst';
  b.append(node('span','episode-nav-department',org.split(' · ')[0]||'Ukendt afdeling'),node('span','episode-nav-label',label+(profile.lastAction?' · '+compactDate(profile.lastAction):'')));
  if(profile.byRole.get('referral').length){
    const referral=node('span','episode-nav-referral');referral.append(icon('referral'),node('span','sr-only','Henvisningsdiagnose registreret'));
    referral.title='Henvisning: '+title(profile.firstReferral||profile.byRole.get('referral')[0].latest);b.firstElementChild.append(referral);
  }
  if(profile.latestActions.length||profile.byRole.get('action').length||profile.overview)b.append(node('span','episode-nav-diagnosis',episodeDiagnosis(profile)));
  if(profile.undatedActions)b.append(node('span','episode-nav-warning','Udateret aktion · seneste usikker'));
  const description=org+' · '+range(profile.episode)+' · Senest opdateret: '+date(updated)+' · '+episodeDiagnosisLabel(profile)+(profile.lastAction?' · '+shortDate(profile.lastAction):'')+' · '+episodeDiagnosis(profile);
  const counts=node('td','episode-nav-counts'),indicators=node('div','episode-count-indicators');
  for(const type of profile.counts){
    const indicator=node('span','episode-content-count tone-'+(type.key==='Notater'||type.key==='Epikriser'?'DocumentReference':type.key));
    indicator.title=type.label+': '+type.count+' hentede poster';indicator.append(icon(type.key),node('span','sr-only',type.label+': '),node('span','',String(type.count)));indicators.append(indicator);
  }
  counts.append(indicators);
  b.title=description;b.setAttribute('aria-label',description+(profile.undatedActions?' · Udateret aktionsdiagnose, seneste usikker':'')+(profile.byRole.get('referral').length?' · Henvisningsdiagnose registreret':''));cell.append(b);row.append(dates,cell,counts);
  row.addEventListener('click',event=>{if(!event.target.closest('button')){setSelection(profile.key);b.focus({preventScroll:true});}});
  return row;
}
function renderEpisodes(){
  const query=$('search').value.trim().toLocaleLowerCase('da');
  const container=$('episodes'),scrollTop=container.scrollTop;container.replaceChildren();episodeYearGroups=[];$('episode-years').replaceChildren();
  $('episode-shortcuts').replaceChildren(selectionButton('Journaloverblik','all','episode-shortcut'),selectionButton('CAVE · '+clinical().filter(isCave).length,'cave','episode-shortcut'));
  const episodes=state.episodes.filter(r=>{
    const p=state.profiles.get(episodeKey(r));
    return !query||searchText(r).includes(query)||[p.overview?.text,p.overview?.coding?.map(c=>c.code).join(' ')].join(' ').toLocaleLowerCase('da').includes(query)||p.items.some(c=>searchText(c).includes(query));
  });
  const table=node('table','episode-nav-table');table.append(node('caption','sr-only','Patientens forløb, senest opdaterede først. År følger opdateringsdatoen. Vælg en afdeling for at læse forløbet.'));
  const head=node('thead'),heading=node('tr');
  for(const label of ['Opdateret / start','Afdeling og diagnose','Hentet indhold']){const th=node('th','',label);th.scope='col';heading.append(th);}head.append(heading);table.append(head);
  let previousYear=null,body=null,yearGroup=null;
  for(const episode of episodes){
    const updated=timestamp(episodeUpdated(episode)),year=updated===null?'Opdateringsdato ikke oplyst':new Intl.DateTimeFormat('da-DK',{year:'numeric',timeZone:'Europe/Copenhagen'}).format(new Date(updated));
    if(year!==previousYear){
      body=node('tbody');const group=node('tr','episode-nav-year'),th=node('th','',year);th.colSpan=3;th.scope='rowgroup';group.append(th);body.append(group);table.append(body);previousYear=year;
      yearGroup={year,body,count:0,activity:0,unknown:updated===null,button:null};episodeYearGroups.push(yearGroup);
    }
    const profile=state.profiles.get(episodeKey(episode));body.append(episodeNavigationRow(profile));yearGroup.count++;yearGroup.activity+=profile.items.length;
  }
  if(episodes.length)container.append(table);
  if(!episodes.length)container.append(node('p','no-results',state.episodes.length?'Ingen forløb matcher søgningen.':'Ingen tilgængelige forløb i udtrækket.'));
  renderYearNavigation();container.scrollTop=scrollTop;updateYearNavigation();
}
function renderYearNavigation(){
  const maxActivity=Math.max(1,...episodeYearGroups.map(group=>group.activity));
  for(const group of episodeYearGroups){
    const b=action('',()=>{
      const container=$('episodes'),header=container.querySelector('thead');
      const top=container.scrollTop+group.body.getBoundingClientRect().top-container.getBoundingClientRect().top-header.getBoundingClientRect().height;
      container.scrollTo({top:Math.max(0,top),behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'});
    },'year-jump');
    b.setAttribute('aria-controls','episodes');b.setAttribute('aria-label',group.year+': '+group.count+' forløb, '+group.activity+' hentede poster. Hop til perioden.');
    b.title=group.year+' · '+group.count+' forløb · '+group.activity+' hentede poster';
    b.append(node('span','year-label',group.unknown?'Ukendt':group.year),node('span','year-episodes',group.count+' forløb'));
    const track=node('span','year-activity-track'),bar=node('span','year-activity-bar');bar.style.width=(100*group.activity/maxActivity)+'%';track.setAttribute('aria-hidden','true');track.append(bar);b.append(track);
    group.button=b;$('episode-years').append(b);
  }
}
function updateYearNavigation(){
  if(!state||!episodeYearGroups.length||$('journal-panel').hidden)return;
  const container=$('episodes');if(!container.clientHeight)return;
  const header=container.querySelector('thead'),limit=container.getBoundingClientRect().top+header.getBoundingClientRect().height+2;
  let current=episodeYearGroups[0];
  for(const group of episodeYearGroups){if(group.body.getBoundingClientRect().top<=limit)current=group;else break;}
  if(container.scrollTop>0&&container.scrollTop+container.clientHeight>=container.scrollHeight-2)current=episodeYearGroups[episodeYearGroups.length-1];
  for(const group of episodeYearGroups){if(group===current)group.button.setAttribute('aria-current','location');else group.button.removeAttribute('aria-current');}
  const nav=$('episode-years'),button=current.button.getBoundingClientRect(),bounds=nav.getBoundingClientRect();
  if(button.top<bounds.top)nav.scrollTop-=bounds.top-button.top;
  else if(button.bottom>bounds.bottom)nav.scrollTop+=button.bottom-bounds.bottom;
}
function renderDetails(){
  const profile=state.profiles.get(selected),episode=profile?.episode;
  $('detail-eyebrow').textContent=episode?'VALGT FORLØB':selected==='cave'?'PATIENTOPLYSNINGER':'PATIENTENS SYGEHISTORIK';
  $('detail-title').textContent=episode?title(episode):selected==='cave'?'CAVE-oplysninger':'Journaloverblik';
  $('detail-subtitle').textContent=episode?range(episode)+' · Senest opdateret: '+date(episodeUpdated(episode))+' · forløbsstatus ikke oplyst':selected==='cave'?'Kildeoplysninger på patientniveau.':'Patientens sygehistorik samlet på ét sted.';
  $('episode-json').hidden=!episode;$('episode-json').onclick=episode?()=>inspect(episode):null;
  const context=$('episode-context');context.replaceChildren();context.hidden=!episode||filter==='summary';
  if(profile){
    context.append(node('span','episode-diagnosis-label',episodeDiagnosisLabel(profile)),node('p','context-diagnosis',episodeDiagnosis(profile)));
    if(profile.undatedActions)context.append(node('p','clinical-note','Aktionsdiagnoser uden dato gør det usikkert, hvilken registrering der er den seneste.'));
  }
  const query=$('detail-search').value.trim().toLocaleLowerCase('da');
  const items=scoped().filter(r=>!query||searchText(r).includes(query));
  $('detail-search-control').hidden=!episode&&selected!=='cave'||filter==='summary';
  const filters=$('filters');filters.replaceChildren();
  filters.hidden=!episode;
  if(episode)for(const tab of journalTabs){
    const count=tab.key==='summary'?null:tab.key==='timeline'?profile.items.length:profile.items.filter(r=>matchesJournalTab(r,tab.key)).length;
    const b=action(tab.label+(count===null?'':' '+count),()=>setJournalTab(tab.key),'filter');b.prepend(icon(tab.key==='summary'?'episode':tab.key==='documents'?'DocumentReference':tab.key));b.setAttribute('aria-pressed',String(filter===tab.key));filters.append(b);
  }
  const container=$('resources');container.replaceChildren();
  if(selected==='all'){
    if(state.episodes.length)renderJournalOverview(container);
    if(!state.episodes.length)container.append(node('p','no-results','Ingen tilgængelige forløb. Eventuelle udeladelser fremgår af datakvalitetsbemærkningerne.'));
  }else if(profile&&filter==='summary')renderEpisodeSummary(profile,container);
  else if(profile&&filter==='timeline')renderTimeline(items,container);
  else{
    const rows=items.filter(r=>selected==='cave'||matchesJournalTab(r,filter)).sort(chronological);
    if(rows.length)container.append(resourceTable(rows,selected==='cave'?'CAVE-oplysninger':journalTabs.find(t=>t.key===filter)?.label||'Forløbsoplysninger'));
    if(!rows.length)container.append(node('p','no-results',query?'Ingen oplysninger matcher søgningen i dette forløb.':'Ingen oplysninger af denne type i det tilgængelige udtræk.'));
  }
}
function matchesJournalTab(r,key){
  if(key==='documents')return r.resourceType==='DocumentReference'&&!isCave(r);
  return r.resourceType===key;
}
function sectionHeading(label,count){const heading=node('div','section-heading');heading.append(node('h3','',label));if(count!==undefined)heading.append(node('span','count',String(count)));return heading;}
function tableShell(columns,label){
  const wrapper=node('div','table-wrap');wrapper.tabIndex=0;wrapper.setAttribute('role','region');wrapper.setAttribute('aria-label',label);
  const table=node('table','clinical-table');table.append(node('caption','sr-only',label));
  const head=node('thead'),row=node('tr');for(const column of columns){const cell=node('th','',column);cell.scope='col';row.append(cell);}head.append(row);
  const body=node('tbody');table.append(head,body);wrapper.append(table);return {wrapper,body};
}
function dateCell(value,end){
  const cell=node('td','table-date');cell.append(node('span','',shortDate(value)));
  if(timestamp(value)!==null){const time=new Intl.DateTimeFormat('da-DK',{timeStyle:'short',timeZone:'Europe/Copenhagen'}).format(new Date(value));cell.append(node('small','table-subtext',time));}
  if(end)cell.append(node('small','table-subtext','Til '+shortDate(end)));return cell;
}
function typeBadge(r){const badge=node('span','type-badge tone-'+r.resourceType);badge.append(icon(r.resourceType),node('span','',timelineLabel(r)));return badge;}
function resourceTable(resources,label){
  const {wrapper,body}=tableShell(['Dato','Oplysning','Type','Kode'],label);
  for(const r of resources){
    const row=node('tr'),description=node('td','table-description'),type=node('td');
    description.append(action(title(r),()=>readResources(title(r),[r]),'table-title'));
    if(r.priority?.text)description.append(node('small','table-subtext',r.priority.text));
    type.append(typeBadge(r));row.append(dateCell(period(r).start,period(r).end),description,type,node('td','table-code',r.code?.coding?.map(c=>c.code).join(' · ')||'—'));body.append(row);
  }
  return wrapper;
}
function diagnosisTable(groups,label,patientWide=false){
  const {wrapper,body}=tableShell(['Diagnose','Art',patientWide?'Sidst identificeret':'Senest registreret',patientWide?'Forløb':'Registreringer'],label);
  for(const group of groups){
    const row=node('tr'),name=node('td','table-description');name.append(action(title(group.latest),()=>readResources(title(group.latest),group.occurrences),'table-title'),node('small','table-code',group.code||'Kode ikke oplyst'));
    const art=node('td','table-art');for(const role of group.roles){const badge=node('span','role-badge role-'+role,({action:'Aktion',secondary:'Bidiagnose',referral:'Henvisning',other:'Anden / ukendt'})[role]);art.append(badge);}
    const last=dateCell(group.lastIdentified);
    if(patientWide&&group.actions.length)last.append(node('small','table-subtext','Som aktion: '+shortDate(group.lastAction)));
    if(group.occurrences.some(r=>!DiagnosisOverview.sourceDate(r)))last.append(node('small','table-warning','Ufuldstændige datoer'));
    row.append(name,art,last,node('td','table-count',String(patientWide?group.episodeCount:group.occurrences.length)));body.append(row);
  }
  return wrapper;
}
function renderJournalOverview(container){
  const intro=node('div','journal-overview-intro');intro.append(icon('timeline'),node('h3','','Find vej i sygehistorien'),node('p','','Vælg et forløb i oversigten. Journalen åbner med diagnoser, kontakter, behandling og notater fra netop dét forløb.'));container.append(intro);
  const counts=node('div','journal-overview-counts');
  for(const type of episodeContentTypes){
    const count=[...state.profiles.values()].reduce((sum,profile)=>sum+profile.counts.find(c=>c.key===type.key).count,0),item=node('div','overview-count tone-'+(type.key==='Notater'||type.key==='Epikriser'?'DocumentReference':type.key));
    item.append(icon(type.key),node('strong','',String(count)),node('span','',type.label));counts.append(item);
  }
  container.append(counts,node('p','clinical-note','Antal viser hentede poster. Manglende detaildata fremgår af datakvalitetsbemærkningerne.'));
  const dates=node('div','overview-dates');dates.append(node('span','','Seneste forløbsopdatering'),node('strong','',date(state.episodes.map(episodeUpdated).find(value=>timestamp(value)!==null))));container.append(dates);
  container.append(node('p','overview-navigation-hint','Brug årstidslinjen til venstre for at hoppe mellem perioder. Bjælkerne viser hentet indhold i forløb opdateret det år.'));
}
function episodeMap(profile){
  const section=node('section','episode-map');section.append(sectionHeading('Forløbet i tid'));
  const dated=profile.items.map(r=>timestamp(period(r).start)).filter(t=>t!==null);
  const min=dated.length?Math.min(...dated):null,max=dated.length?Math.max(...dated):null;
  const axis=node('div','map-axis');axis.append(node('span','',min===null?'Datoer ikke oplyst':shortDate(new Date(min).toISOString())),node('span','',max===null?'':shortDate(new Date(max).toISOString())));section.append(axis);
  for(const [type,label,key] of [['Condition','Diagnoser','Condition'],['Encounter','Kontakter','Encounter'],['Procedure','Procedurer','Procedure'],['DocumentReference','Dokumenter','documents']]){
    const items=profile.items.filter(r=>r.resourceType===type),lane=action('',()=>setJournalTab(key),'map-row tone-'+type);
    lane.setAttribute('aria-label',label+': '+items.length+' registreringer. Vis oplysninger.');
    const heading=node('span','map-label');heading.append(icon(type),node('span','',label));
    const track=node('span','map-track');track.setAttribute('aria-hidden','true');
    const moments=new Set(items.map(r=>timestamp(period(r).start)).filter(t=>t!==null));
    for(const moment of moments){const dot=node('span','map-dot');dot.style.left=(max===min?50:5+90*(moment-min)/(max-min))+'%';track.append(dot);}
    lane.append(heading,track,node('span','map-count',String(items.length)));section.append(lane);
  }
  const missing=profile.items.length-dated.length;section.append(node('p','map-note','Kilderegistreringer'+(missing?' · '+missing+' uden dato':'')+' · vælg et spor for at se tabellen'));
  return section;
}
function renderEpisodeSummary(profile,container){
  const warning=state.warnings.filter(w=>w.episodeKey===profile.key);
  if(warning.length){const details=node('details','episode-quality');details.append(node('summary','',warning.length+' bemærkninger om dette forløbs data'));const list=node('ul');list.append(...warning.map(w=>node('li','',w.message)));details.append(list);container.append(details);}
  container.append(episodeMap(profile));
  const core=[...profile.byRole.get('referral'),...profile.byRole.get('action')];
  const diagnoses=node('section','episode-diagnosis-summary');diagnoses.append(sectionHeading('Henvisning og aktionsdiagnoser',core.length));
  if(core.length)diagnoses.append(diagnosisTable(core,'Henvisnings- og aktionsdiagnoser i det valgte forløb'));
  else diagnoses.append(node('p','clinical-note','Ingen henvisnings- eller aktionsdiagnoser i detailregistreringerne.'));
  if(profile.undatedActions)diagnoses.append(node('p','table-warning','Aktionsdiagnoser uden dato gør den seneste registrering usikker.'));
  const other=[...profile.byRole.get('secondary'),...profile.byRole.get('other')];
  if(other.length){const details=node('details','summary-disclosure');details.append(node('summary','','Bidiagnoser og øvrige diagnoser · '+other.length));details.append(diagnosisTable(other,'Bidiagnoser og øvrige diagnoser i det valgte forløb'));diagnoses.append(details);}
  if(profile.overview){
    const fallback=node('div','overview-diagnosis');fallback.append(node('span','eyebrow','FORLØBSOVERSIGTENS DIAGNOSE'),node('strong','',conceptTitle(profile.overview)));
    if(core.length){const details=node('details','summary-disclosure');details.append(node('summary','','Diagnose fra forløbsoversigten'));details.append(fallback);diagnoses.append(details);}else diagnoses.append(fallback);
  }
  container.append(diagnoses);
  const explanation=node('details','summary-disclosure');explanation.append(node('summary','','Om datoer og kildegrundlag'),node('p','clinical-note','Diagnosekoder samles pr. art med seneste DatoFra. Oversigtsdiagnosen har ingen oplyst diagnoseart eller diagnosedato. Forløbssporet viser daterede kilderegistreringer; det angiver ikke sygdomsdebut, aktivitet eller gennemført behandling. Samtidige poster er ikke nødvendigvis knyttet til samme kontaktperiode.'));
  if(profile.firstReferral)explanation.append(node('p','clinical-note','Tidligste daterede henvisningsdiagnose: '+title(profile.firstReferral)+' · '+date(DiagnosisOverview.sourceDate(profile.firstReferral))));
  container.append(explanation);
}
function timelineLabel(r){
  if(r.resourceType==='Condition')return (r.category||[]).map(c=>c.text).filter(Boolean).join(' · ')||'Diagnose · art ikke oplyst';
  if(r.resourceType==='Encounter')return 'Kontaktperiode';if(r.resourceType==='Procedure')return 'Procedure';
  return sourceType(r)==='Epikriser'?'Epikrise':sourceType(r)==='Notater'?'Notat':'Dokument';
}
function renderTimeline(items,container){
  const timeline=node('div','episode-timeline');let previousDay=null;
  const rank=r=>r.resourceType==='Condition'&&DiagnosisOverview.role(r)==='referral'?0:r.resourceType==='Encounter'?1:r.resourceType==='Condition'?2:r.resourceType==='Procedure'?3:4;
  const sorted=[...items].sort((a,b)=>{
    const left=timestamp(period(a).start),right=timestamp(period(b).start);
    if(left===right)return rank(a)-rank(b)||String(a.id).localeCompare(String(b.id));return chronological(a,b);
  });
  for(const r of sorted){
    const time=timestamp(period(r).start),day=time===null?'Uden registreringsdato':dayFormatter.format(new Date(time));
    if(day!==previousDay){timeline.append(node('h3','timeline-day',day));previousDay=day;}
    const event=node('section','timeline-event tone-'+r.resourceType),entry=action('',()=>readResources(title(r),[r]),'timeline-entry');
    entry.append(node('span','timeline-type',timelineLabel(r)),node('strong','',title(r)));
    if(time!==null)entry.append(node('small','table-subtext',new Intl.DateTimeFormat('da-DK',{timeStyle:'short',timeZone:'Europe/Copenhagen'}).format(new Date(time))));
    event.append(icon(r.resourceType),entry);timeline.append(event);
  }
  if(!items.length)timeline.append(node('p','no-results','Ingen hændelser i dette valg.'));container.append(timeline);
}
function resourceCard(r){
  const card=node('article','resource-card');const top=node('div','card-top');const main=node('div');
  main.append(node('span','date',range(r)),node('h4','',title(r)));top.append(main,action('FHIR ↗',()=>inspect(r)));card.append(top);
  const badges=node('div','badges');
  if(r.resourceType==='Encounter'||r.resourceType==='Procedure')badges.append(node('span','badge unknown','Status ikke oplyst'));
  if(r.priority?.text)badges.append(node('span','badge',r.priority.text));
  if(r.category)for(const c of (Array.isArray(r.category)?r.category:[r.category]))if(c.text)badges.append(node('span','badge',c.text));
  if(r.resourceType==='DocumentReference')badges.append(node('span','badge',sourceType(r)));
  if(badges.childElementCount)card.append(badges);
  for(const coding of r.code?.coding||[])card.append(node('span','code',coding.code));
  const org=organization(r);if(org)card.append(node('p','organization',org));
  for(const additional of r.extension||[]){
    if(additional.url===local+'source-additional-code')card.append(node('p','organization','Tillægskode: '+[additional.valueCodeableConcept?.coding?.[0]?.code,additional.valueCodeableConcept?.text].filter(Boolean).join(' · ')));
  }
  const note=ext(r,'source-text')?.valueString;if(note)card.append(node('p','document-text',note));
  if(r.resourceType==='DocumentReference'){
    const text=documentText(r);card.append(node('p','document-preview',text.slice(0,200)+(text.length>200?'…':'')));
    const details=node('details');details.append(node('summary','','Læs hele dokumentet'),node('div','document-text',text));card.append(details);
  }
  return card;
}
function showView(view){
  const diagnoses=view==='diagnoses';
  $('diagnosis-panel').hidden=!diagnoses;$('journal-panel').hidden=diagnoses;
  $('diagnosis-view').setAttribute('aria-pressed',String(diagnoses));$('journal-view').setAttribute('aria-pressed',String(!diagnoses));
}
function renderDiagnoses(){
  const groups=state.diagnoses;
  const query=$('diagnosis-search').value.trim().toLocaleLowerCase('da');
  const chapter=$('diagnosis-chapter').value;
  const base=groups.filter(g=>(!query||g.occurrences.some(r=>searchText(r).includes(query)))&&(chapter==='all'||g.classification.key===chapter));
  const matchesRole=g=>diagnosisRole==='all'||g.roles.includes(diagnosisRole);
  const matchesKind=g=>diagnosisKind==='all'||g.classification.kind===diagnosisKind;
  const stats=$('diagnosis-stats');stats.replaceChildren();
  for(const kind of [{key:'all',label:'Alle diagnosekoder'},...DiagnosisOverview.kinds]){
    const count=base.filter(g=>matchesRole(g)&&(kind.key==='all'||g.classification.kind===kind.key)).length;
    const button=action('',()=>{diagnosisKind=kind.key;renderDiagnoses();},'diagnosis-stat');
    button.setAttribute('aria-pressed',String(diagnosisKind===kind.key));
    button.append(node('span','stat-label',kind.label),node('span','stat-number',String(count)));stats.append(button);
  }
  const roles=$('diagnosis-roles');roles.replaceChildren();
  for(const role of [{key:'all',label:'Alle diagnosearter'},...DiagnosisOverview.roles]){
    const count=base.filter(g=>matchesKind(g)&&(role.key==='all'||g.roles.includes(role.key))).length;
    const button=action(role.label+' '+count,()=>{diagnosisRole=role.key;renderDiagnoses();},'filter');
    button.setAttribute('aria-pressed',String(diagnosisRole===role.key));roles.append(button);
  }
  const visible=base.filter(g=>matchesRole(g)&&matchesKind(g));
  $('diagnosis-result-count').textContent=visible.length+' diagnosekoder · '+visible.reduce((sum,g)=>sum+g.occurrences.length,0)+' registreringer';
  const container=$('diagnosis-groups');container.replaceChildren();
  // Områder sorteres efter deres seneste registrering; datoer sammenlignes som tidspunkter.
  const sections=new Map();
  for(const group of visible){
    const {key,label,kind}=group.classification;
    if(!sections.has(key)){
      const section=node('section','diagnosis-area');const heading=node('div','section-heading');
      heading.append(node('h3','',label),node('span','badge',DiagnosisOverview.kinds.find(k=>k.key===kind).label));
      section.append(heading);sections.set(key,{section,groups:[]});container.append(section);
    }
    sections.get(key).groups.push(group);
  }
  for(const {section,groups} of sections.values())section.append(diagnosisTable(groups,'Diagnoser på tværs af patientens forløb',true));
  if(!visible.length)container.append(node('p','no-results',groups.length?'Ingen diagnoser matcher dine filtre.':'Ingen diagnoseregistreringer i det tilgængelige udtræk. Det betyder ikke, at patienten er uden sygdom.'));
}
function showJournal(result){
  const resources=(result.bundle.entry||[]).map(e=>e.resource);
  state={...result,resources,diagnoses:DiagnosisOverview.group(resources),byUrl:new Map((result.bundle.entry||[]).map(e=>[e.fullUrl,e.resource])),episodes:resources.filter(r=>r.resourceType==='EpisodeOfCare').sort(latestUpdatedFirst)};
  state.profiles=buildEpisodeProfiles();
  selected='all';filter='summary';$('search').value='';$('detail-search').value='';$('episodes').scrollTop=0;$('episode-years').scrollTop=0;$('detail-panel-body').scrollTop=0;
  diagnosisRole='all';diagnosisKind='all';$('diagnosis-search').value='';
  const available=new Map(state.diagnoses.map(g=>[g.classification.key,g.classification.label]));
  $('diagnosis-chapter').replaceChildren(...[{key:'all',label:'Alle områder'},...[...available].map(([key,label])=>({key,label})).sort((a,b)=>a.label.localeCompare(b.label,'da'))].map(ch=>{const option=node('option','',ch.label);option.value=ch.key;return option;}));
  $('diagnosis-chapter').value='all';showView('journal');
  const patient=resources.find(r=>r.resourceType==='Patient');const name=patient?.name?.[0]?.text||'Navn ikke oplyst';
  $('patient-name').textContent=name;$('initials').textContent=name.split(/\s+/).slice(0,2).map(n=>n[0]).join('');
  const cpr=patient?.identifier?.find(i=>i.system==='urn:oid:1.2.208.176.1.2')?.value||'';
  $('patient-cpr').textContent='CPR '+(cpr.length===10?cpr.slice(0,6)+'-'+cpr.slice(6):cpr);
  $('resource-total').textContent=state.episodes.length+' forløb i udtrækket';$('episode-count').textContent=String(state.episodes.length);
  $('quality').hidden=!result.warnings.length;$('quality').open=false;
  $('quality-summary').textContent=result.warnings.length+' bemærkninger om datakvalitet'+(result.omittedEpisodes?' · '+result.omittedEpisodes+' forløb udeladt':'');
  $('warnings').replaceChildren(...result.warnings.map(w=>node('li','',w.message+(w.episodeKey?' (forløb '+w.episodeKey+')':''))));
  $('empty-state').hidden=true;$('journal').hidden=false;document.body.classList.add('has-journal');render();renderDiagnoses();
}
$('patient-form').addEventListener('submit',async event=>{
  event.preventDefault();$('load').disabled=true;$('feedback').className='feedback';$('feedback').textContent='Henter forløb og detailoplysninger fra SDK-demoen…';$('journal').setAttribute('aria-busy','true');
  // Fjern en eventuel tidligere patient, også hvis den nye forespørgsel fejler.
  state=null;$('journal').hidden=true;$('empty-state').hidden=false;document.body.classList.remove('has-journal');$('reader').close();$('reader-title').textContent='';$('reader-content').replaceChildren();$('inspector').close();$('json').textContent='';
  try{
    const cpr=$('cpr').value.trim();
    const response=await fetch('/api/fhir/'+encodeURIComponent(cpr),{cache:'no-store',headers:{Accept:'application/json'}});
    const result=await response.json();if(!response.ok)throw new Error(result.message||'Data kunne ikke hentes.');
    showJournal(result);$('feedback').textContent='Journalen er hentet · '+state.episodes.length+' forløb · '+result.fhirVersion+' FHIR-version';
  }catch(error){$('feedback').className='feedback error';$('feedback').textContent=error.message||'API’et kunne ikke kontaktes.';}
  finally{$('load').disabled=false;$('journal').setAttribute('aria-busy','false');}
});
$('search').addEventListener('input',()=>{if(state)renderEpisodes();});
let yearScrollPending=false;
$('episodes').addEventListener('scroll',()=>{if(yearScrollPending)return;yearScrollPending=true;requestAnimationFrame(()=>{yearScrollPending=false;updateYearNavigation();});},{passive:true});
const yearNavigationResize=new ResizeObserver(()=>updateYearNavigation());yearNavigationResize.observe($('episodes'));
$('return-overview').addEventListener('click',()=>{if(!state)return;setSelection('all');$('search').focus({preventScroll:true});});
$('detail-search').addEventListener('input',()=>{if(state)renderDetails();});
$('diagnosis-search').addEventListener('input',()=>{if(state)renderDiagnoses();});
$('diagnosis-chapter').addEventListener('change',()=>{if(state)renderDiagnoses();});
$('diagnosis-view').addEventListener('click',()=>showView('diagnoses'));
$('journal-view').addEventListener('click',()=>showView('journal'));
$('close-inspector').addEventListener('click',()=>$('inspector').close());
$('close-reader').addEventListener('click',()=>$('reader').close());
$('download').addEventListener('click',()=>{
  if(!state)return;const url=URL.createObjectURL(new Blob([JSON.stringify(state.bundle,null,2)],{type:'application/fhir+json'}));
  const anchor=node('a');anchor.href=url;anchor.download='ejournal-fhir-bundle.json';anchor.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
});
})();
