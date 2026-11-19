const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');
const assets = path.resolve(__dirname, '../../mobile/android/fenix/app/src/main/assets/extensions/penrix');

test('chapter extraction retains source before cleaning and conversion; dynamic text converts and restores', async () => {
  const dom = new JSDOM(`<!doctype html><html lang="zh-TW"><body>
    <a href="/book/88637/index.html">測試作品</a>
    <a href="/book/88637.html">書頁</a><a href="/book/88637/index.html">目錄</a>
    <div class="txtnav"><h1>第二章 門</h1><div>作者：測試作者</div>
      <div id="txtcontent0">他推開門。<br>追台灣小說就上台灣小說網，t̴w̴k̴a̴n̴.c̴o̴m̴輕鬆讀<br>屋裡沒有人。</div>
    </div><a href="/txt/88637/50790316">下一章</a><textarea>保留繁體輸入</textarea>
  </body></html>`, { url:'https://twkan.com/txt/88637/50790315', runScripts:'outside-only' });
  const { window } = dom;
  const messages = [];
  let handler;
  window.browser = {
    storage:{ local:{ get:async () => ({ simplifiedHosts:[] }) } },
    runtime:{ onMessage:{ addListener:fn => { handler = fn; } }, connectNative:() => ({ postMessage:message => messages.push(message) }) },
  };
  for (const file of ['opencc.js', 'text.js', 'content.js']) window.eval(fs.readFileSync(path.join(assets, file), 'utf8'));
  await new Promise(resolve => setTimeout(resolve, 0));
  const chapter = messages.find(m => m.type === 'chapter_ready').chapter;
  assert.match(chapter.rawText, /台灣小說網/);
  assert.equal(chapter.workTitle, '测试作品');
  assert.equal(chapter.text, '他推开门。\n\n屋里没有人。');
  assert.equal(chapter.nextUrl, 'https://twkan.com/txt/88637/50790316');
  assert.equal(window.document.querySelector('textarea').value, '保留繁體輸入');
  const added = window.document.createElement('p');
  added.textContent = '繁體內容';
  window.document.body.append(added);
  await new Promise(resolve => setTimeout(resolve, 0));
  assert.equal(added.textContent, '繁体内容');
  handler({ type:'penrix-text', action:'original' });
  assert.equal(added.textContent, '繁體內容');
  window.close();
});
