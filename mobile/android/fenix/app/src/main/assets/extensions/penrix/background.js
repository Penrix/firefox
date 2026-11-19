browser.webRequest.onBeforeRequest.addListener(
  ({ documentUrl }) => ({ cancel: /^https?:\/\/(?:[^/]+\.)?cool18\.com\/bbs4\//i.test(documentUrl || '') }),
  { urls: ['*://ad.parkvv.com/*'], types: ['script', 'xmlhttprequest'] },
  ['blocking'],
);

async function inject(tabId) {
  for (const file of ['opencc.js', 'text.js', 'content.js']) {
    await browser.tabs.executeScript(tabId, { file });
  }
}
browser.tabs.onUpdated.addListener(async (tabId, change, tab) => {
  if (change.status !== 'complete' || !/^https?:/.test(tab.url || '')) return;
  const host = new URL(tab.url).hostname;
  const { simplifiedHosts = [] } = await browser.storage.local.get('simplifiedHosts');
  if (!simplifiedHosts.includes(host) || /(^|\.)(twkan\.com|cool18\.com)$/.test(host)) return;
  try { await inject(tabId); } catch (error) { console.warn('Penrix conversion:', error.message); }
});
browser.runtime.onMessage.addListener(async message => {
  if (message.type !== 'penrix-inject') return undefined;
  await inject(message.tabId);
  return { ok:true };
});
