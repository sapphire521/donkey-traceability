# -*- coding: utf-8 -*-
"""
将 Playwright 测试录屏合成为带中文配音解说的讲解视频。
用法: python make_video.py mapping.json output.mp4
mapping.json: [{"title": "章节标题", "text": "解说词", "files": ["video1.webm", ...]}, ...]
画质升级版: 1920x1080, CRF16, 单轮高质量编码
"""
import asyncio, json, os, subprocess, sys, tempfile
import edge_tts
import imageio_ffmpeg
from PIL import Image, ImageDraw, ImageFont

FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
VOICE = "zh-CN-YunxiNeural"
W, H = 1920, 1080
CRF = "16"
PRESET = "faster"
FONT_PATH = r"C:\Windows\Fonts\msyh.ttc"

def get_duration(path):
    """用 ffmpeg -i stderr 解析时长（webm/mp4 通用）"""
    p = subprocess.run([FFMPEG, "-i", path], capture_output=True, text=True, encoding="utf-8", errors="ignore")
    import re
    m = re.search(r"Duration:\s*(\d+):(\d+):(\d+\.(\d+))", p.stderr)
    if m:
        h, mi, s = int(m.group(1)), int(m.group(2)), float(m.group(3))
        return h * 3600 + mi * 60 + s
    return 0.0

async def tts(text, out):
    await edge_tts.Communicate(text, VOICE, rate="-5%").save(out)

def title_png(text, out, subtitle=""):
    img = Image.new("RGBA", (W, 170), (15, 23, 42, 216))
    d = ImageDraw.Draw(img)
    f1 = ImageFont.truetype(FONT_PATH, 54)
    f2 = ImageFont.truetype(FONT_PATH, 27)
    d.rectangle([(0, 0), (12, 170)], fill=(34, 197, 94, 255))
    d.text((56, 38 if subtitle else 56), text, font=f1, fill=(255, 255, 255, 255))
    if subtitle:
        d.text((56, 110), subtitle, font=f2, fill=(190, 242, 200, 255))
    img.save(out)

def intro_png(out):
    img = Image.new("RGB", (W, H), (11, 46, 39))
    d = ImageDraw.Draw(img)
    for y in range(H):
        g = int(11 + y / H * 10)
        d.line([(0, y), (W, y)], fill=(g, 46 + int(y / H * 8), 39 + int(y / H * 6)))
    f1 = ImageFont.truetype(FONT_PATH, 82)
    f2 = ImageFont.truetype(FONT_PATH, 36)
    f3 = ImageFont.truetype(FONT_PATH, 29)
    d.text((W // 2, 390), "驴肉火烧区块链溯源管理系统", font=f1, fill=(255, 255, 255), anchor="mm")
    d.text((W // 2, 500), "全功能演示与自动化测试讲解", font=f2, fill=(134, 239, 172), anchor="mm")
    d.text((W // 2, 670), "Vue 3 + Element Plus · Hyperledger Fabric 联盟链存证 · Playwright 全功能测试", font=f3, fill=(148, 163, 184), anchor="mm")
    d.text((W // 2, 740), "驴链 DonkeyTrace Admin", font=f3, fill=(148, 163, 184), anchor="mm")
    img.save(out)

def outro_png(out):
    img = Image.new("RGB", (W, H), (11, 46, 39))
    d = ImageDraw.Draw(img)
    f1 = ImageFont.truetype(FONT_PATH, 72)
    f2 = ImageFont.truetype(FONT_PATH, 34)
    d.text((W // 2, 460), "演示完毕 · 谢谢观看", font=f1, fill=(255, 255, 255), anchor="mm")
    d.text((W // 2, 570), "全部功能测试通过 · 区块链存证完整可信", font=f2, fill=(134, 239, 172), anchor="mm")
    d.text((W // 2, 650), "运行方式: cd admin-web && npm run dev → http://localhost:5173", font=f2, fill=(148, 163, 184), anchor="mm")
    img.save(out)

def run(cmd):
    r = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="ignore")
    if r.returncode != 0:
        print("FFMPEG FAIL:", " ".join(cmd)[:400])
        print(r.stderr[-1500:])
        raise SystemExit(1)

def build_segment(files, text, title, tmp, idx):
    """单个章节: 可能含多个视频文件，先顺序拼接再配音叠字幕条"""
    sub = os.path.join(tmp, f"seg{idx}")
    os.makedirs(sub, exist_ok=True)
    # 1. 拼接原始片段
    merged = os.path.join(sub, "raw.mp4")
    if len(files) == 1 and files[0].endswith(".mp4"):
        merged = files[0]
    else:
        lst = os.path.join(sub, "list.txt")
        norm = []
        for i, f in enumerate(files):
            nf = os.path.join(sub, f"n{i}.mp4")
            run([FFMPEG, "-y", "-i", f, "-vf", f"scale={W}:{H}:force_original_aspect_ratio=decrease,pad={W}:{H}:(ow-iw)/2:(oh-ih)/2,fps=30",
                 "-c:v", "libx264", "-preset", PRESET, "-crf", CRF, "-pix_fmt", "yuv420p", "-an", nf])
            norm.append(nf)
        with open(lst, "w", encoding="utf-8") as fh:
            for nf in norm:
                fh.write("file '" + nf.replace("\\", "/").replace("'", "'\\''") + "'\n")
        run([FFMPEG, "-y", "-f", "concat", "-safe", "0", "-i", lst, "-c", "copy", merged])

    # 2. 配音
    mp3 = os.path.join(sub, "voice.mp3")
    asyncio.run(tts(text, mp3))
    p = subprocess.run([FFMPEG, "-i", mp3], capture_output=True, text=True, encoding="utf-8", errors="ignore")
    import re
    m = re.search(r"Duration:\s*(\d+):(\d+):(\d+\.(\d+))", p.stderr)
    tts_dur = (int(m.group(1)) * 3600 + int(m.group(2)) * 60 + float(m.group(3))) if m else 0

    v_dur = get_duration(merged)
    total = max(v_dur + 0.6, tts_dur + 1.6, 4.0)
    pad = max(0.0, total - v_dur)

    # 3. 字幕条
    png = os.path.join(sub, "title.png")
    title_png(title, png)

    # 4. 合成: 视频 tpad 补尾帧 + 字幕条叠加 + 配音 apad
    out = os.path.join(sub, "final.mp4")
    cmd = [FFMPEG, "-y", "-i", merged, "-i", png, "-i", mp3, "-filter_complex",
           f"[0:v]tpad=stop_mode=clone:stop_duration={pad:.2f},fps=30[v0];"
           f"[v0][1:v]overlay=(W-w)/2:H-h-64[v1];"
           f"[2:a]apad[aud]",
           "-map", "[v1]", "-map", "[aud]", "-t", f"{total:.2f}",
           "-c:v", "libx264", "-preset", PRESET, "-crf", CRF, "-pix_fmt", "yuv420p",
           "-c:a", "aac", "-b:a", "128k", "-ar", "44100", out]
    run(cmd)
    return out

def main():
    mapping_path, out_path = sys.argv[1], sys.argv[2]
    with open(mapping_path, encoding="utf-8") as f:
        segments = json.load(f)
    tmp = os.path.join(os.path.dirname(out_path) or ".", "_video_tmp")
    os.makedirs(tmp, exist_ok=True)
    finals = []

    # 片头
    intro = os.path.join(tmp, "intro.png")
    intro_png(intro)
    intro_mp3 = os.path.join(tmp, "intro.mp3")
    asyncio.run(tts("欢迎使用驴肉火烧区块链溯源管理系统。本视频由自动化测试全程驱动，完整演示平台从养殖建档、屠宰加工、冷链运输、门店销售，到区块链存证与监管召回的全部功能。", intro_mp3))
    intro_out = os.path.join(tmp, "intro.mp4")
    run([FFMPEG, "-y", "-loop", "1", "-i", intro, "-i", intro_mp3,
         "-filter_complex", "[1:a]apad[aud]", "-map", "0:v", "-map", "[aud]",
         "-t", "10", "-r", "30", "-c:v", "libx264", "-preset", PRESET, "-crf", CRF, "-pix_fmt", "yuv420p",
         "-c:a", "aac", "-b:a", "128k", "-ar", "44100", intro_out])
    finals.append(intro_out)

    # 正文章节
    for i, seg in enumerate(segments):
        print(f"[{i+1}/{len(segments)}] {seg['title']}")
        seg_files = [f for f in seg["files"] if os.path.exists(f)]
        if not seg_files:
            print("  !! 无可用视频文件，跳过:", seg["files"])
            continue
        finals.append(build_segment(seg_files, seg["text"], seg["title"], tmp, i))

    # 片尾
    outro = os.path.join(tmp, "outro.png")
    outro_png(outro)
    outro_mp3 = os.path.join(tmp, "outro.mp3")
    asyncio.run(tts("以上就是本系统的全部功能演示。所有模块测试通过，区块链账本完整性校验通过。感谢观看。", outro_mp3))
    outro_out = os.path.join(tmp, "outro.mp4")
    run([FFMPEG, "-y", "-loop", "1", "-i", outro, "-i", outro_mp3,
         "-filter_complex", "[1:a]apad[aud]", "-map", "0:v", "-map", "[aud]",
         "-t", "8", "-r", "30", "-c:v", "libx264", "-preset", PRESET, "-crf", CRF, "-pix_fmt", "yuv420p",
         "-c:a", "aac", "-b:a", "128k", "-ar", "44100", outro_out])
    finals.append(outro_out)

    # 最终拼接
    lst = os.path.join(tmp, "final_list.txt")
    with open(lst, "w", encoding="utf-8") as fh:
        for f in finals:
            fh.write("file '" + f.replace("\\", "/").replace("'", "'\\''") + "'\n")
    run([FFMPEG, "-y", "-f", "concat", "-safe", "0", "-i", lst, "-c", "copy", out_path])
    print("DONE:", out_path, os.path.getsize(out_path) // 1024, "KB")

if __name__ == "__main__":
    main()
