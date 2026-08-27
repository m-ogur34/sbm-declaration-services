# mock-esb.ps1
# ESB ve alz-token-management yerine gecen sahte sunucu.
# Gelen her istegin metodunu, path'ini, header'larini ve govdesini ekrana basar.
# Kullanim:  powershell -ExecutionPolicy Bypass -File .\mock-esb.ps1
# Durdurmak icin Ctrl+C

$port = 8089

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:$port/")
$listener.Start()

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Green
Write-Host " Mock sunucu dinliyor: http://localhost:$port/" -ForegroundColor Green
Write-Host " Durdurmak icin Ctrl+C" -ForegroundColor Green
Write-Host "=====================================================" -ForegroundColor Green
Write-Host ""

try {
    while ($listener.IsListening) {
        $ctx = $listener.GetContext()
        $req = $ctx.Request

        $reader = New-Object System.IO.StreamReader($req.InputStream, [System.Text.Encoding]::UTF8)
        $body = $reader.ReadToEnd()
        $reader.Close()

        $path = $req.Url.AbsolutePath
        $method = $req.HttpMethod

        Write-Host ""
        Write-Host "===== $method $path =====" -ForegroundColor Cyan

        Write-Host "--- HEADERS ---" -ForegroundColor Yellow
        foreach ($h in $req.Headers.AllKeys) {
            Write-Host ("  {0}: {1}" -f $h, $req.Headers[$h])
        }

        Write-Host "--- BODY ---" -ForegroundColor Yellow
        if ([string]::IsNullOrWhiteSpace($body)) {
            Write-Host "  (GOVDE BOS)" -ForegroundColor Red
        } else {
            try {
                $body | ConvertFrom-Json | ConvertTo-Json -Depth 10
            } catch {
                Write-Host $body
            }
        }

        # --- Cevap secimi ---
        if ($path -like "*token*") {
            # alz-token-management cevabi
            $json = '{"accessToken":"mock-access-token-abc123","clientCredentials":{"clientIdentityType":1,"clientIdNumber":"9990000001"}}'
            $status = 200
        }
        elseif ($method -eq "POST") {
            # SBM gonderim cevabi - SBM'nin verdigi YENI bicim (data sarmalayici)
            $json = '{"result":true,"status":201,"data":{"ysvDosyaNo":"TEST-YSV-202607-001"}}'
            $status = 201
        }
        elseif ($method -eq "PUT") {
            # SBM guncelleme cevabi
            $json = '{"result":true,"status":200,"data":true}'
            $status = 200
        }
        else {
            # SBM sorgu cevabi
            $json = '{"result":true,"status":200,"sigortaSirketKodu":"045","sonOdemeTarihi":"2026-08-31","ysvDosyaNo":"TEST-YSV-202607-001","ysvTutarList":[{"alinanPrimTutari":1,"iptalPrimTutari":1,"menkulTipi":"MENKUL","odenecekVergi":25000,"vergiOrani":10,"vergiPrimTutari":250000}]}'
            $status = 200
        }

        Write-Host "--- CEVAP ($status) ---" -ForegroundColor Green
        Write-Host "  $json"

        $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
        $ctx.Response.StatusCode = $status
        $ctx.Response.ContentType = "application/json; charset=utf-8"
        $ctx.Response.Headers.Add("Transaction-Id", [guid]::NewGuid().ToString())
        $ctx.Response.ContentLength64 = $bytes.Length
        $ctx.Response.OutputStream.Write($bytes, 0, $bytes.Length)
        $ctx.Response.Close()
    }
}
finally {
    $listener.Stop()
    $listener.Close()
    Write-Host ""
    Write-Host "Mock sunucu durduruldu." -ForegroundColor Green
}
