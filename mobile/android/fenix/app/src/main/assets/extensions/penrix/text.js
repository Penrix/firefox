(function (root) {
  'use strict';
  const OpenCC = typeof module === 'object' ? require('./opencc.js') : root.OpenCC;
  const convert = OpenCC.Converter({ from: 'twp', to: 'cn' });
  const mainlandPhrases = OpenCC.CustomConverter([
    ['打开软体', '打开软件'], ['安装软体', '安装软件'], ['下载软体', '下载软件'],
    ['随身碟', 'U盘'], ['档案存到随身碟', '文件存到U盘'], ['档案存到U盘', '文件存到U盘'], ['资料库', '数据库'],
    ['网路', '网络'], ['影片还在下载', '视频还在下载'], ['下载影片', '下载视频'],
  ]);
  const cues = ['写到这里','寫到這裡','读者记一下','讀者記一下','请记住','請記住','本书首发','本書首發','记住','記住','首发','首發','本站域名','网站域名','網站域名','我们域名','我們域名','找台湾小说','找台灣小說','追台湾小说','追台灣小說','读台湾','讀台灣','看台湾','看台灣','台湾好书','台灣好書','追台湾小说首选','追台灣小說首選','看台湾小说认准','看台灣小說認準'];
  const tails = ['超全','超赞','超讚','超靠谱','超靠譜','超给力','超給力','超实用','超實用','超省心','超贴心','超貼心','超顺畅','超順暢','随时看','隨時看','轻松看','輕鬆看','轻松读','輕鬆讀','章节更新','章節更新','无错章节','無錯章節','无乱序','無亂序','阅读体验','閱讀體驗','观看最快的章节更新','觀看最快的章節更新','提供给你无错章节无乱序章节的阅读体验','提供給你無錯章節無亂序章節的閱讀體驗'];
  const folds = { '臺':'台','説':'說','❻':'6','➅':'6','➏':'6','❾':'9','➈':'9','➒':'9','ѕ':'s','丂':'s','н':'h','ħ':'h','ย':'u','א':'x','х':'x','с':'c','ο':'o','о':'o','σ':'o','м':'m' };
  function skeleton(raw, offset = 0) {
    let text = '', index = 0;
    const starts = [], ends = [];
    for (const piece of raw) {
      const folded = piece.normalize('NFKD').replace(/\p{M}/gu, '').normalize('NFKC').toLowerCase();
      for (const ch of folded) {
        const out = folds[ch] || ch;
        if (/[\p{L}\p{N}]/u.test(out)) {
          text += out;
          for (let unit = 0; unit < out.length; unit++) { starts.push(offset + index); ends.push(offset + index + piece.length); }
        }
      }
      index += piece.length;
    }
    return { text, starts, ends };
  }
  const cueKeys = cues.map(v => skeleton(v).text);
  const weakCues = new Set(['记住','記住','首发','首發','读台湾','讀台灣','看台湾','看台灣']);
  const tailKeys = tails.map(v => skeleton(v).text).sort((a, b) => b.length - a.length);
  function cleanTwkan(rawText) {
    const spans = [];
    function add(mapped, start, end, reason, decoration = false) {
      let a = mapped.starts[start], b = mapped.ends[end - 1];
      if (a === undefined || b === undefined) return;
      const decorative = /[\s\p{M}\p{S}【】〖〗〔〕〈〉《》（）()\[\]{}•◦]/u;
      if (decoration) {
        while (a > 0) {
          const before = Array.from(rawText.slice(0, a)).pop();
          if (!decorative.test(before)) break;
          a -= before.length;
        }
        while (b < rawText.length) {
          const after = String.fromCodePoint(rawText.codePointAt(b));
          if (!decorative.test(after)) break;
          b += after.length;
        }
      } else {
        while (b < rawText.length && /\p{M}/u.test(String.fromCodePoint(rawText.codePointAt(b)))) b++;
      }
      spans.push({ start:a, end:b, reason });
    }
    for (const match of rawText.matchAll(/[^\n。！？!?；;]+/gu)) {
      const mapped = skeleton(match[0], match.index), s = mapped.text;
      for (const credit of s.matchAll(/本[書书]由twkancom全[網网]首[發发]/g)) {
        add(mapped, credit.index, credit.index + credit[0].length, 'twkan-first-publication');
      }
      for (const marker of ['googlesearchtwkan','google搜索twkan','台湾小说网藏书广twkancom任你读','台灣小說網藏書廣twkancom任你讀','台灣小說網藏書广twkancom任你读','台湾小说网藏书廣twkancom任你讀']) {
        for (let at = s.indexOf(marker); at >= 0; at = s.indexOf(marker, at + marker.length)) add(mapped, at, at + marker.length, 'observed-watermark');
      }
      for (const marker of ['69shuxcom','69shux']) {
        for (let at = s.indexOf(marker); at >= 0; at = s.indexOf(marker, at + marker.length)) {
          const chunk = rawText.slice(mapped.starts[at], mapped.ends[at + marker.length - 1]);
          const residue = s.slice(0, at) + s.slice(at + marker.length);
          if (/[^\x00-\x7f]/u.test(chunk) || (!/\p{Script=Han}/u.test(residue) && residue.length <= 24)) add(mapped, at, at + marker.length, 'legacy-watermark', true);
        }
      }
      for (const site of ['twkancom','台湾小说网','台灣小說網']) {
        for (let at = s.indexOf(site); at >= 0; at = s.indexOf(site, at + site.length)) {
          const matches = [];
          for (const cue of cueKeys) {
            for (let i = s.indexOf(cue); i >= 0 && i < at; i = s.indexOf(cue, i + cue.length)) {
              if (at - i <= 100) matches.push({ start:i, end:i + cue.length, cue });
            }
          }
          matches.sort((a, b) => b.end - a.end || a.start - b.start);
          if (!matches.length) continue;
          let start = matches[0].start, end = at + site.length;
          for (;;) {
            const earlier = matches.find(m => m.start < start && (m.end >= start || start - m.end <= (weakCues.has(m.cue) ? 0 : 6)));
            if (!earlier) break;
            start = earlier.start;
          }
          for (;;) {
            const tail = tailKeys.find(t => s.startsWith(t, end));
            if (!tail) break;
            end += tail.length;
          }
          add(mapped, start, end, 'twkan-promotion');
        }
      }
      const domainAt = s.indexOf('twkancom');
      const nameAt = Math.max(s.indexOf('台湾小说网'), s.indexOf('台灣小說網'));
      if (domainAt >= 0 && nameAt >= 0) {
        const start = Math.min(domainAt, nameAt), end = Math.max(domainAt + 8, nameAt + 5);
        const residue = s.slice(0, start) + s.slice(end);
        if (residue.length <= 20 && !/\p{Script=Han}/u.test(residue)) add(mapped, start, end, 'twkan-identity');
      }
    }
    spans.sort((a, b) => a.start - b.start);
    const removedSpans = [];
    for (const span of spans) {
      const last = removedSpans[removedSpans.length - 1];
      if (last && span.start <= last.end) last.end = Math.max(last.end, span.end);
      else removedSpans.push({ ...span });
    }
    let cleanText = rawText;
    for (const span of [...removedSpans].reverse()) cleanText = cleanText.slice(0, span.start) + cleanText.slice(span.end);
    return { rawText, cleanText:removedSpans.length ? cleanText.replace(/[ \t\u3000]+\n/g, '\n').trim() : rawText, removedSpans, cleanerVersion:5 };
  }
  const api = { cleanTwkan, simplify:text => mainlandPhrases(convert(text)) };
  if (typeof module === 'object') module.exports = api;
  else root.PenrixText = api;
})(globalThis);
