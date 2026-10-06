// Browser smoke verification: offline navigation, searching and all declaration links.
const { chromium } = require('C:/Users/ker/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const fs = require('fs');
const path = require('path');
(async () => {
  const dir = path.resolve(__dirname, '../docs/medcom/modelatlas');
  const data = JSON.parse(fs.readFileSync(path.join(dir, 'atlas.json'), 'utf8'));
  const browser = await chromium.launch({headless:true,channel:'msedge'});
  const page = await browser.newPage({ viewport:{width:1440,height:1000} });
  const errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.goto('file:///'+path.join(dir,'index.html').replaceAll('\\','/'));
  await page.getByRole('heading',{name:'Hvor stor er SUP-modellen?'}).waitFor();
  await page.screenshot({path:path.join(dir,'overview.png'),fullPage:true});
  for(const tab of ['catalog','reuse','types','sources','transition','method']) {
    await page.evaluate(t=>{location.hash=t},tab);
    await page.waitForFunction(t=>location.hash==='#'+t,tab);
    await page.waitForTimeout(60);
    if(!(await page.locator('#app').innerText()).trim())throw Error('Empty tab '+tab);
  }
  await page.evaluate(()=>{location.hash='catalog'});
  await page.locator('#q').fill('Notat');
  if(!(await page.locator('#results').innerText()).includes('Notat'))throw Error('Search failed');
  for(const r of data.records) {
    await page.evaluate(id=>{location.hash='item/'+encodeURIComponent(id)},r.id);
    await page.waitForFunction(id=>document.querySelector('article h2')?.textContent===id,r.name);
  }
  const note=data.records.find(r=>r.global&&r.name==='Notat');
  await page.evaluate(id=>{location.hash='item/'+encodeURIComponent(id)},note.id);
  await page.waitForTimeout(100);
  await page.screenshot({path:path.join(dir,'notat.png'),fullPage:true});
  await page.setViewportSize({width:390,height:844});
  await page.screenshot({path:path.join(dir,'mobile.png'),fullPage:true});
  if(errors.length)throw Error(errors.join('\n'));
  await browser.close();
  console.log(`Verified ${data.records.length} declaration pages, 7 tabs, search and desktop/mobile rendering; no browser errors.`);
})().catch(e=>{console.error(e);process.exit(1)});
