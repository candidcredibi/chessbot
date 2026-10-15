package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

class ChessBridge(
    private val onBoardFenDetected: (String, Boolean) -> Unit
) {
    @JavascriptInterface
    fun reportBoardState(fen: String, isFlipped: Boolean) {
        if (fen.isNotBlank()) {
            onBoardFenDetected(fen, isFlipped)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChessComWebView(
    url: String,
    onBoardDetected: (String, Boolean) -> Unit,
    onWebViewCreated: (WebView) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bridge = remember { ChessBridge(onBoardDetected) }

    AndroidView(
        factory = { ctx ->
            // Prevent corrupted Chromium code cache error
            try {
                val codeCacheDir = java.io.File(ctx.cacheDir, "WebView/Default/HTTP Cache/Code Cache")
                if (!codeCacheDir.exists()) {
                    codeCacheDir.mkdirs()
                }
            } catch (e: Exception) {}

            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // If running on virtual emulator without DRI render nodes, fallback to software rendering
                val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic") ||
                        android.os.Build.FINGERPRINT.startsWith("unknown") ||
                        android.os.Build.HARDWARE.contains("goldfish") ||
                        android.os.Build.HARDWARE.contains("ranchu") ||
                        android.os.Build.PRODUCT.contains("sdk")

                if (isEmulator) {
                    setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                addJavascriptInterface(bridge, "AndroidChessBridge")

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        injectChessObserver(view)
                    }
                }

                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            // Keep webview updated
        },
        modifier = modifier
    )
}

fun injectChessObserver(webView: WebView?) {
    val script = """
        (function() {
            if (window._chessObserverInjected) return;
            window._chessObserverInjected = true;

            function extractBoardFen() {
                try {
                    // Try wc-chess-board element
                    var boardEl = document.querySelector('wc-chess-board') || document.querySelector('.board');
                    if (!boardEl) return null;

                    var isFlipped = boardEl.classList.contains('flipped') || boardEl.getAttribute('flipped') === 'true';

                    // Scan pieces
                    var grid = [];
                    for (var r = 0; r < 8; r++) {
                        grid[r] = ['', '', '', '', '', '', '', ''];
                    }

                    var pieceEls = boardEl.querySelectorAll('.piece');
                    if (!pieceEls || pieceEls.length === 0) return null;

                    pieceEls.forEach(function(el) {
                        var classes = el.className.split(' ');
                        var typeChar = '';
                        var isWhite = false;
                        var file = -1, rank = -1;

                        classes.forEach(function(c) {
                            if (c.length === 2 && (c[0] === 'w' || c[0] === 'b')) {
                                isWhite = c[0] === 'w';
                                var pType = c[1].toUpperCase();
                                typeChar = isWhite ? pType : pType.toLowerCase();
                            }
                            if (c.indexOf('square-') === 0) {
                                var sqNum = c.replace('square-', '');
                                if (sqNum.length === 2) {
                                    file = parseInt(sqNum[0]) - 1;
                                    rank = parseInt(sqNum[1]) - 1;
                                }
                            }
                        });

                        if (file >= 0 && file < 8 && rank >= 0 && rank < 8 && typeChar) {
                            grid[rank][file] = typeChar;
                        }
                    });

                    // Build FEN rows
                    var fenRows = [];
                    for (var r = 7; r >= 0; r--) {
                        var emptyCount = 0;
                        var rowStr = '';
                        for (var f = 0; f < 8; f++) {
                            var p = grid[r][f];
                            if (p === '') {
                                emptyCount++;
                            } else {
                                if (emptyCount > 0) {
                                    rowStr += emptyCount;
                                    emptyCount = 0;
                                }
                                rowStr += p;
                            }
                        }
                        if (emptyCount > 0) rowStr += emptyCount;
                        fenRows.push(rowStr);
                    }

                    // Guess turn from highlight or moves list
                    var turn = 'w';
                    var moveList = document.querySelector('.move-list-move-item-active');
                    if (moveList && moveList.classList.contains('black')) turn = 'b';

                    var fen = fenRows.join('/') + ' ' + turn + ' KQkq - 0 1';
                    if (window.AndroidChessBridge) {
                        window.AndroidChessBridge.reportBoardState(fen, isFlipped);
                    }
                    return fen;
                } catch(e) {
                    return null;
                }
            }

            // Continuous observation
            var target = document.body;
            var observer = new MutationObserver(function() {
                extractBoardFen();
            });
            observer.observe(target, { childList: true, subtree: true, attributes: true });

            // Initial check
            setTimeout(extractBoardFen, 1500);
            setTimeout(extractBoardFen, 3500);
        })();
    """.trimIndent()

    webView?.evaluateJavascript(script, null)
}

fun renderArrowOnChessComBoard(webView: WebView?, bestMoveNotation: String, isFlipped: Boolean) {
    if (webView == null || bestMoveNotation.length < 4) return
    val cleanMove = bestMoveNotation.take(4).lowercase()
    val script = """
        (function() {
            try {
                var board = document.querySelector('wc-chess-board') || document.querySelector('.board');
                if (!board) return;

                var existingSvg = document.getElementById('sf-arrow-overlay');
                if (existingSvg) existingSvg.remove();

                var move = '$cleanMove';
                var fromFile = move.charCodeAt(0) - 97;
                var fromRank = parseInt(move[1]) - 1;
                var toFile = move.charCodeAt(2) - 97;
                var toRank = parseInt(move[3]) - 1;

                if (isNaN(fromFile) || isNaN(fromRank) || isNaN(toFile) || isNaN(toRank)) return;

                var flipped = $isFlipped || board.classList.contains('flipped') || board.getAttribute('flipped') === 'true';

                var fCol = flipped ? (7 - fromFile) : fromFile;
                var fRow = flipped ? fromRank : (7 - fromRank);
                var tCol = flipped ? (7 - toFile) : toFile;
                var tRow = flipped ? toRank : (7 - toRank);

                var x1 = (fCol + 0.5) * 12.5;
                var y1 = (fRow + 0.5) * 12.5;
                var x2 = (tCol + 0.5) * 12.5;
                var y2 = (tRow + 0.5) * 12.5;

                // Adjust endpoint slightly so arrowhead doesn't overshoot
                var dx = x2 - x1;
                var dy = y2 - y1;
                var dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 3) {
                    x2 = x2 - (dx / dist) * 2.0;
                    y2 = y2 - (dy / dist) * 2.0;
                }

                var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                svg.setAttribute('id', 'sf-arrow-overlay');
                svg.setAttribute('viewBox', '0 0 100 100');
                svg.setAttribute('style', 'position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:99999;');

                svg.innerHTML = '<defs>' +
                    '<marker id="sf-arrowhead" markerWidth="6" markerHeight="6" refX="4" refY="3" orient="auto">' +
                    '<polygon points="0 0, 6 3, 0 6" fill="#00E676" />' +
                    '</marker>' +
                    '</defs>' +
                    '<circle cx="' + x1 + '" cy="' + y1 + '" r="3" fill="#00E676" opacity="0.5" />' +
                    '<line x1="' + x1 + '" y1="' + y1 + '" x2="' + x2 + '" y2="' + y2 + '" stroke="#00E676" stroke-width="2.6" stroke-linecap="round" marker-end="url(#sf-arrowhead)" opacity="0.95" />';

                board.appendChild(svg);
            } catch(e) {}
        })();
    """.trimIndent()

    webView.evaluateJavascript(script, null)
}
