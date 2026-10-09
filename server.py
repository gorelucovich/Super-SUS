import os
import sys
import http.server
import socketserver
import time

PORT = 3000
APK_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".build-outputs", "app-debug.apk")

HTML_PAGE = """<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Скачать SusRadar APK</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: #0A0E17; color: #F9FAFB; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; }
        .card { background: #111827; border: 1px solid #1F2937; border-radius: 24px; padding: 36px 28px; max-width: 440px; width: 100%; text-align: center; box-shadow: 0 20px 40px rgba(0,0,0,0.5); }
        .icon { width: 72px; height: 72px; background: rgba(99, 102, 241, 0.15); border: 2px solid #6366F1; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; font-size: 32px; }
        h1 { font-size: 24px; font-weight: 800; margin-bottom: 8px; color: #FFFFFF; }
        p { font-size: 14px; color: #9CA3AF; margin-bottom: 24px; line-height: 1.5; }
        .btn { display: inline-flex; align-items: center; justify-content: center; width: 100%; padding: 16px 24px; background: #4F46E5; color: #FFFFFF; border-radius: 16px; font-size: 16px; font-weight: 700; text-decoration: none; transition: background 0.2s; box-shadow: 0 4px 14px rgba(79, 70, 229, 0.4); }
        .btn:hover { background: #4338CA; }
        .note { font-size: 12px; color: #6B7280; margin-top: 18px; line-height: 1.4; }
        .status { margin-top: 14px; font-size: 13px; color: #10B981; font-weight: 600; }
    </style>
</head>
<body>
    <div class="card">
        <div class="icon">📡</div>
        <h1>SusRadar APK</h1>
        <p>AI Детектор ролей и Радар для Super Sus с оверлеем поверх игры и распознаванием речи.</p>
        <a href="/SusRadar.apk" class="btn" id="downloadBtn">📥 СКАЧАТЬ SUSRADAR (.APK)</a>
        <div class="status" id="statusText">⏳ Скачивание начнётся автоматически...</div>
        <p class="note">Если загрузка не началась автоматически, нажмите на кнопку выше.</p>
    </div>
    <script>
        setTimeout(function() {
            window.location.href = '/SusRadar.apk';
            document.getElementById('statusText').innerText = '✅ Загрузка запущена!';
        }, 1200);
    </script>
</body>
</html>
"""

class RobustApkHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        # Prevent noisy logs from throwing BrokenPipe on broken client
        try:
            super().log_message(format, *args)
        except Exception:
            pass

    def do_HEAD(self):
        try:
            if self.path in ['/SusRadar.apk', '/app-debug.apk', '/download']:
                if os.path.exists(APK_PATH):
                    self.send_response(200)
                    self.send_header('Content-Type', 'application/vnd.android.package-archive')
                    self.send_header('Content-Disposition', 'attachment; filename="SusRadar.apk"')
                    self.send_header('Content-Length', str(os.path.getsize(APK_PATH)))
                    self.send_header('Accept-Ranges', 'bytes')
                    self.end_headers()
                else:
                    self.send_error(404, "APK not found")
            else:
                self.send_response(200)
                self.send_header('Content-Type', 'text/html; charset=utf-8')
                self.send_header('Content-Length', str(len(HTML_PAGE.encode('utf-8'))))
                self.end_headers()
        except (BrokenPipeError, ConnectionResetError):
            pass
        except Exception as e:
            try:
                self.send_error(500, str(e))
            except Exception:
                pass

    def do_GET(self):
        try:
            if self.path in ['/SusRadar.apk', '/app-debug.apk', '/download']:
                if os.path.exists(APK_PATH):
                    file_size = os.path.getsize(APK_PATH)
                    self.send_response(200)
                    self.send_header('Content-Type', 'application/vnd.android.package-archive')
                    self.send_header('Content-Disposition', 'attachment; filename="SusRadar.apk"')
                    self.send_header('Content-Length', str(file_size))
                    self.send_header('Accept-Ranges', 'bytes')
                    self.end_headers()
                    with open(APK_PATH, 'rb') as f:
                        while chunk := f.read(65536):
                            try:
                                self.wfile.write(chunk)
                            except (BrokenPipeError, ConnectionResetError):
                                break
                else:
                    self.send_error(404, "APK file not found")
            else:
                content = HTML_PAGE.encode('utf-8')
                self.send_response(200)
                self.send_header('Content-Type', 'text/html; charset=utf-8')
                self.send_header('Content-Length', str(len(content)))
                self.end_headers()
                self.wfile.write(content)
        except (BrokenPipeError, ConnectionResetError):
            pass
        except Exception as e:
            try:
                self.send_error(500, str(e))
            except Exception:
                pass

class ThreadedTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    allow_reuse_address = True
    daemon_threads = True

def run_server():
    while True:
        try:
            with ThreadedTCPServer(('0.0.0.0', PORT), RobustApkHandler) as httpd:
                print(f"Serving APK on port {PORT}...", flush=True)
                httpd.serve_forever()
        except Exception as e:
            print(f"Server restart due to: {e}", flush=True)
            time.sleep(1)

if __name__ == '__main__':
    run_server()
