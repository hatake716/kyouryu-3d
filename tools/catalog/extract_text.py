"""Prepare attributed Japanese Wikipedia excerpts for editorial review (not a build input fetch)."""
import re, json
from pathlib import Path
from urllib.parse import quote
from lxml import html
from research import CACHE, clean
EXCLUDE={'脚注','出典','参考文献','関連項目','外部リンク','注釈','参考資料','関連作品','登場作品','大衆文化','文化における'}
def sections(taxon,ja):
    root=html.fromstring((CACHE/f'ja-{ja}.html').read_bytes())
    body=max(root.xpath('//*[contains(concat(" ",normalize-space(@class)," ")," mw-parser-output ")]'),key=lambda e:len(e.text_content()))
    # Keep the article's uncertainty labels; discard unsourced assertions marked 要出典.
    for e in body.xpath('.//sup[contains(@class,"reference")]|.//span[contains(@class,"mw-editsection")]'):e.drop_tree()
    collected=[];heading='概要';parts=[];skip=False
    def flush():
        if parts:collected.append({'title':heading,'text':'\n\n'.join(parts)})
    for e in body.iter():
        if e.tag in ('h2','h3'):
            flush();parts=[];heading=clean(e.text_content());skip=any(x in heading for x in EXCLUDE)
        if e.tag!='p' or skip or e.xpath('ancestor::table|ancestor::figure|ancestor::div[contains(@class,"navbox")]'):continue
        t=clean(e.text_content())
        if len(t)<30 or any(s in t for s in ['要出典','リンク先の画像','howjsay.com','この節の加筆']):continue
        parts.append(t)
    flush()
    return collected
if __name__=='__main__':
    for p in CACHE.glob('*-research.json'):
        r=json.loads(p.read_text());ja=r.get('japanese')
        if ja and r.get('jaArticle'):
            out=sections(r['title'],ja)
            (CACHE/f"{r['title']}-sections.json").write_text(json.dumps(out,ensure_ascii=False,indent=2))
            print(r['title'],len(out),sum(len(s['text']) for s in out))
