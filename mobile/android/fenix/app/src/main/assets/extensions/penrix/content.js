(async function () {
  if (globalThis.__penrixReading) return;
  globalThis.__penrixReading = true;
  const twkan = /(^|\.)twkan\.com$/.test(location.hostname);
  const cool18 = /(^|\.)cool18\.com$/.test(location.hostname) && location.pathname.startsWith('/bbs4/');
  const originals = new Map();
  let simplified = false, observer;
  const excluded = 'script,style,textarea,input,select,code,pre,[contenteditable]:not([contenteditable="false"]),[data-penrix-controls]';
  function convert(root = document.body) {
    if (!root) return;
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    const nodes = [];
    while (walker.nextNode()) if (!walker.currentNode.parentElement?.closest(excluded)) nodes.push(walker.currentNode);
    for (const node of nodes) {
      const current = node.nodeValue;
      const saved = originals.get(node);
      if (saved && current === saved.converted) continue;
      const converted = PenrixText.simplify(current);
      if (converted !== current) {
        originals.set(node, { raw:current, converted });
        node.nodeValue = converted;
      }
    }
    document.documentElement.lang = 'zh-CN';
  }
  function setSimplified(enabled) {
    simplified = enabled;
    if (enabled) {
      convert();
      if (!observer) {
        observer = new MutationObserver(records => {
          for (const record of records) {
            if (record.type === 'characterData') convert(record.target.parentElement);
            else for (const node of record.addedNodes) {
              if (node.nodeType === Node.ELEMENT_NODE) convert(node);
              else if (node.nodeType === Node.TEXT_NODE) convert(node.parentElement);
            }
          }
        });
      }
      observer.observe(document.body, { childList:true, subtree:true, characterData:true });
    } else {
      observer?.disconnect();
      for (const [node, value] of originals) if (node.isConnected && node.nodeValue === value.converted) node.nodeValue = value.raw;
      originals.clear();
      document.documentElement.lang = originalLanguage;
    }
  }
  const originalLanguage = document.documentElement.lang;
  browser.runtime.onMessage.addListener(message => {
    if (message.type === 'penrix-text') setSimplified(message.action !== 'original');
  });
  function plainText(root) {
    const clone = root.cloneNode(true);
    clone.querySelectorAll('script,style,iframe,ins,.adsbygoogle,[data-penrix-controls]').forEach(n => n.remove());
    let out = '';
    function walk(node) {
      if (node.nodeType === Node.TEXT_NODE) out += node.nodeValue;
      else {
        const block = /^(BR|P|DIV|LI|SECTION|H[1-6])$/.test(node.nodeName);
        if (block && out && !out.endsWith('\n')) out += '\n';
        node.childNodes.forEach(walk);
        if (block && out && !out.endsWith('\n')) out += '\n';
      }
    }
    walk(clone);
    return out.replace(/\n[ \t\u3000]+/g, '\n').replace(/\n{3,}/g, '\n\n').trim();
  }
  function extract() {
    const route = location.pathname.match(/^\/(?:txt|book)\/(\d+)\/(\d+)(?:\.html)?\/?$/);
    let root, workUrl, title, workTitle;
    if (twkan && route) {
      root = document.querySelector('#txtcontent0, #txtcontent, .txtnav');
      workUrl = location.origin + '/book/' + route[1] + '.html';
      title = document.querySelector('h1')?.textContent?.trim() || document.title;
      workTitle = document.querySelector('meta[property="og:novel:book_name"]')?.content || document.querySelector('a[href$="/book/' + route[1] + '.html"]')?.textContent?.trim() || title;
    } else if (cool18 && new URL(location.href).searchParams.get('act') === 'threadview') {
      root = document.querySelector('#threadcontent');
      workUrl = location.href;
      title = document.querySelector('h1')?.textContent?.trim() || document.title;
      workTitle = title;
    }
    if (!root) return null;
    const rawText = plainText(root);
    if (!rawText) return null;
    const cleaned = twkan ? PenrixText.cleanTwkan(rawText) : { rawText, cleanText:rawText, removedSpans:[], cleanerVersion:0 };
    function chapterLink(label) {
      const link = [...document.querySelectorAll('a[href]')].find(a => label.test(a.textContent.trim()));
      if (!link) return null;
      const target = new URL(link.href);
      if (!twkan || !route) return target.origin === location.origin ? target.href : null;
      const next = target.pathname.match(/^\/(?:txt|book)\/(\d+)\/(\d+)(?:\.html)?\/?$/);
      return target.origin === location.origin && next && next[1] === route[1] && next[2] !== route[2] ? target.href : null;
    }
    return { siteId:twkan ? 'twkan' : 'cool18', url:location.href, workUrl, title:PenrixText.simplify(title), workTitle:PenrixText.simplify(workTitle), rawText, text:PenrixText.simplify(cleaned.cleanText), removedSpans:cleaned.removedSpans, cleanerVersion:cleaned.cleanerVersion, previousUrl:chapterLink(/^上一[章頁页]$/), nextUrl:chapterLink(/^下一[章頁页]$/), catalogUrl:twkan ? location.origin + '/book/' + route[1] + '/index.html' : workUrl };
  }
  const chapter = extract();
  if (chapter) {
    if (twkan) {
      const root = document.querySelector('#txtcontent0, #txtcontent, .txtnav');
      const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
      const nodes = [];
      while (walker.nextNode()) nodes.push(walker.currentNode);
      for (const node of nodes) {
        const cleaned = PenrixText.cleanTwkan(node.nodeValue);
        if (cleaned.removedSpans.length) node.nodeValue = cleaned.cleanText;
      }
    }
    const port = browser.runtime.connectNative('penrixReading');
    port.postMessage({ type:'chapter_ready', chapter });
    const bar = document.createElement('div');
    bar.dataset.penrixControls = 'true';
    bar.style.cssText = 'position:fixed;right:10px;bottom:16px;z-index:2147483647;display:flex;gap:6px;padding:7px;background:#edf4ee;border:1px solid #cad8cd;border-radius:12px;box-shadow:0 2px 12px #0002';
    for (const [label, action] of [['阅读', () => port.postMessage({ type:'read', chapter })], ['收藏', () => port.postMessage({ type:'save', chapter })], ['简体/原文', () => setSimplified(!simplified)]]) {
      const button = document.createElement('button');
      button.textContent = label;
      button.style.cssText = 'font:15px sans-serif;color:#164e36;background:white;border:0;border-radius:7px;padding:10px';
      button.addEventListener('click', action);
      bar.append(button);
    }
    document.body.append(bar);
  }
  const { simplifiedHosts = [] } = await browser.storage.local.get('simplifiedHosts');
  if (twkan || cool18 || simplifiedHosts.includes(location.hostname)) setSimplified(true);
})();
