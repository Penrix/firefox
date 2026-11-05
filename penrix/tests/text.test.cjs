const { test } = require('node:test');
const assert = require('node:assert/strict');
const text = require('../../mobile/android/fenix/app/src/main/assets/extensions/penrix/text.js');

test('removes observed TWKAN Unicode promotion, retaining surrounding prose', () => {
  const raw = '他推開門。追台灣小說就上台灣小說網，t̴̴w̴̴k̴̴̴a̴̴n̴̴.c̴̴o̴̴m̴̴輕鬆讀\n屋裡沒有人。';
  const result = text.cleanTwkan(raw);
  assert.equal(result.cleanText, '他推開門。\n屋裡沒有人。');
  assert.equal(result.rawText, raw);
  assert.equal(result.removedSpans.length, 1);
});

test('removes legacy and literal watermarks without removing dialogue', () => {
  const raw = '他說：記住這個名字，等會兒再走，本站域名twkan.com超全然後他推門出去。';
  assert.equal(text.cleanTwkan(raw).cleanText, '他說：記住這個名字，等會兒再走，然後他推門出去。');
  assert.equal(text.cleanTwkan('甲。GOOGLE搜索TWKAN乙。').cleanText, '甲。乙。');
  assert.equal(text.cleanTwkan('甲。🍧♣ ❻➈ŜⒽ𝕦𝐗.𝒸σ𝓶 ♠😺「乙。」').cleanText, '甲。「乙。」');
});

test('keeps ordinary site discussion, names and special punctuation', () => {
  for (const raw of [
    '他用 Google 搜索了一下 TWKAN 之外的資料，沒有把這當成廣告。',
    '他說台湾小说网藏书广，但他并不喜欢twkan.com这个域名。',
    '記住這個名字，然後繼續走。',
    '〖念能力：認知之門〗「乾坤未定。」',
    '  正文原有縮排。\n',
  ]) assert.equal(text.cleanTwkan(raw).cleanText, raw);
});

test('preserves rare name characters beside a watermark and removes standalone site identity', () => {
  assert.equal(text.cleanTwkan('𠮷野推門。GOOGLE搜索TWKAN𠮷野回頭。').cleanText, '𠮷野推門。𠮷野回頭。');
  assert.equal(text.cleanTwkan('台灣小說網 twkan.com').cleanText, '');
});

test('cleans observed library slogan and does not consume normal following prose', () => {
  assert.equal(text.cleanTwkan('前文。台湾小说网藏书广，𝗍𝗐𝗄𝖺𝗇.𝖼𝗈𝗆任你读后文。').cleanText, '前文。后文。');
  assert.equal(text.cleanTwkan('前文本站域名twkan.com後文提到閱讀體驗很好。').cleanText, '前文後文提到閱讀體驗很好。');
});

test('simplifies Taiwanese variants and phrases, retains meaningful characters', () => {
  assert.equal(text.simplify('他在裡面使用滑鼠和軟體，閱讀著作，乾坤未定。'), '他在里面使用鼠标和软件，阅读著作，乾坤未定。');
});
