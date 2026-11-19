for (const action of ['simplify', 'automatic', 'original']) {
  document.getElementById(action).addEventListener('click', async () => {
    const status = document.getElementById('status');
    try {
      const [tab] = await browser.tabs.query({ active:true, currentWindow:true });
      if (!tab || !/^https?:/.test(tab.url)) throw new Error('请先打开网页');
      if (action === 'automatic') {
        const { simplifiedHosts = [] } = await browser.storage.local.get('simplifiedHosts');
        await browser.storage.local.set({ simplifiedHosts:[...new Set([...simplifiedHosts, new URL(tab.url).hostname])] });
      }
      await browser.runtime.sendMessage({ type:'penrix-inject', tabId:tab.id });
      await browser.tabs.sendMessage(tab.id, { type:'penrix-text', action });
      status.textContent = action === 'original' ? '已显示原文' : action === 'automatic' ? '此网站以后自动转简体' : '已转为简体';
    } catch (error) { status.textContent = error.message; }
  });
}
