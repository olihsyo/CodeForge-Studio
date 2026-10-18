// CodeForge Studio - Offline Touch-Optimized Editor Engine
// Supports 14 Programming Languages with Real Syntax Highlighting, Touch Gestures,
// Bracket Matching, Line Numbering, Auto-Indent, Diagnostics, and Android Bridge.

(function() {
    'use strict';

    let currentLanguage = 'plaintext';
    let currentTheme = 'dark';
    let tabSize = 4;
    let wordWrap = false;
    let undoStack = [];
    let redoStack = [];
    let isApplyingHistory = false;
    let searchMatches = [];
    let currentSearchIndex = -1;
    let diagnostics = [];

    const codeEditor = document.getElementById('code-editor');
    const highlightLayer = document.getElementById('highlight-layer');
    const gutter = document.getElementById('gutter');
    const codeWrapper = document.getElementById('code-wrapper');

    // Language Token Patterns
    const LANGUAGE_RULES = {
        python: {
            keywords: /\b(def|class|if|elif|else|for|while|try|except|finally|with|as|import|from|return|yield|break|continue|pass|raise|lambda|async|await|assert|global|nonlocal|in|is|not|and|or)\b/g,
            types: /\b(int|float|str|bool|list|dict|set|tuple|bytes|None|True|False|self|cls)\b/g,
            comments: /(#.*$)/gm,
            strings: /("""[\s\S]*?"""|'''[\s\S]*?'''|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')/g,
            numbers: /\b\d+(\.\d+)?([eE][+-]?\d+)?\b/g,
            functions: /\b([a-zA-Z_]\w*)(?=\s*\()/g,
            operators: /[+\-*/%=&|^~<>!]/g
        },
        javascript: {
            keywords: /\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|try|catch|finally|throw|new|class|extends|import|export|default|from|async|await|yield|typeof|instanceof|void|delete|in|of)\b/g,
            types: /\b(true|false|null|undefined|this|super|window|document|console|Promise|Array|Object|String|Number|Boolean|Map|Set)\b/g,
            comments: /(\/\*[\s\S]*?\*\/|\/\/.*$)/gm,
            strings: /(`(?:\\.|[^`\\])*`|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')/g,
            numbers: /\b0x[0-9a-fA-F]+\b|\b\d+(\.\d+)?\b/g,
            functions: /\b([a-zA-Z_$][\w$]*)(?=\s*\()/g,
            operators: /[+\-*/%=&|^~<>!?:.]/g
        },
        typescript: {
            keywords: /\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|try|catch|finally|throw|new|class|extends|implements|interface|type|enum|namespace|module|import|export|default|from|async|await|yield|typeof|instanceof|as|is|keyof|readonly)\b/g,
            types: /\b(string|number|boolean|any|unknown|never|void|null|undefined|this|super|Promise|Array|Record|Partial)\b/g,
            comments: /(\/\*[\s\S]*?\*\/|\/\/.*$)/gm,
            strings: /(`(?:\\.|[^`\\])*`|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')/g,
            numbers: /\b0x[0-9a-fA-F]+\b|\b\d+(\.\d+)?\b/g,
            functions: /\b([a-zA-Z_$][\w$]*)(?=\s*[<(])/g,
            operators: /[+\-*/%=&|^~<>!?:.]/g
        },
        kotlin: {
            keywords: /\b(val|var|fun|class|interface|object|enum|sealed|data|open|abstract|override|private|protected|public|internal|if|else|when|for|while|do|return|break|continue|throw|try|catch|finally|import|package|is|as|in|by|companion|init|constructor|suspend|inline)\b/g,
            types: /\b(Int|Long|Float|Double|String|Boolean|Char|Byte|Short|Unit|Nothing|Any|List|Map|Set|Array|true|false|null|this|super)\b/g,
            comments: /(\/\*[\s\S]*?\*\/|\/\/.*$)/gm,
            strings: /("""[\s\S]*?"""|"(?:\\.|[^"\\])*")/g,
            numbers: /\b\d+([uU]?[lL]?|[fF]?)\b/g,
            functions: /\b([a-zA-Z_]\w*)(?=\s*[<(])/g,
            operators: /[+\-*/%=&|^~<>!?:.]/g
        },
        java: {
            keywords: /\b(public|protected|private|static|final|abstract|class|interface|enum|extends|implements|new|return|if|else|for|while|do|switch|case|default|break|continue|try|catch|finally|throw|throws|import|package|synchronized|volatile|transient|native|instanceof|void|super|this)\b/g,
            types: /\b(int|long|float|double|boolean|char|byte|short|String|Object|Integer|Long|List|Map|Set|true|false|null)\b/g,
            comments: /(\/\*[\s\S]*?\*\/|\/\/.*$)/gm,
            strings: /("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')/g,
            numbers: /\b\d+(\.\d+)?([fFdDlL])?\b/g,
            functions: /\b([a-zA-Z_]\w*)(?=\s*[<(])/g,
            operators: /[+\-*/%=&|^~<>!?:.]/g
        },
        c: {
            keywords: /\b(auto|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|int|long|register|return|short|signed|sizeof|static|struct|switch|typedef|union|unsigned|void|volatile|while)\b/g,
            types: /\b(NULL|true|false|size_t|uint8_t|uint16_t|uint32_t|uint64_t|int8_t|int16_t|int32_t|int64_t|FILE)\b/g,
            comments: /(\/\*[\s\S]*?\*\/|\/\/.*$)/gm,
            strings: /("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')/g,
            numbers: /\b0x[0-9a-fA-F]+\b|\b\d+(\.\d+)?\b/g,
            functions: /\b([a-zA-Z_]\w*)(?=\s*\()/g,
            operators: /[+\-*/%=&|^~<>!?:.]/g
        },
        cpp: {
            keywords: /\b(auto|break|case|class|const|constexpr|continue|default|delete|do|else|enum|explicit|export|extern|for|friend|goto|if|inline|mutable|namespace|new|noexcept|operator|override|private|protected|public|return|sizeof|static|struct|switch|template|this|throw|try|catch|typedef|typename|union|using|virtual|volatile|while)\b/g,
            types: /\b(int|long|float|double|char|bool|void|std::string|std::vector|std::map|nullptr|true|false)\b/g,
            comments: /(\/\*[\s\S]*?\*\/|\/\/.*$)/gm,
            strings: /("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')/g,
            numbers: /\b\d+(\.\d+)?\b/g,
            functions: /\b([a-zA-Z_]\w*)(?=\s*[<(])/g,
            operators: /[+\-*/%=&|^~<>!?:.]/g
        },
        rust: {
            keywords: /\b(as|break|const|continue|crate|else|enum|extern|fn|for|if|impl|in|let|loop|match|mod|move|mut|pub|ref|return|self|Self|static|struct|super|trait|type|unsafe|use|where|while|async|await|dyn)\b/g,
            types: /\b(i8|i16|i32|i64|i128|isize|u8|u16|u32|u64|u128|usize|f32|f64|bool|char|str|String|Option|Result|Some|None|Ok|Err|true|false)\b/g,
            comments: /(\/\*[\s\S]*?\*\/|\/\/.*$)/gm,
            strings: /("(?:\\.|[^"\\])*"|r#"[^"]*"#)/g,
            numbers: /\b\d+(_?\d+)*([uif]\d+)?\b/g,
            functions: /\b([a-zA-Z_]\w*)(?=\s*[<(])/g,
            operators: /[+\-*/%=&|^~<>!?:.]/g
        },
        go: {
            keywords: /\b(break|case|chan|const|continue|default|defer|else|fallthrough|for|func|go|goto|if|import|interface|map|package|range|return|select|struct|switch|type|var)\b/g,
            types: /\b(int|int8|int16|int32|int64|uint|uint8|uint16|uint32|uint64|uintptr|float32|float64|complex64|complex128|string|bool|byte|rune|error|nil|true|false)\b/g,
            comments: /(\/\*[\s\S]*?\*\/|\/\/.*$)/gm,
            strings: /(`[^`]*`|"(?:\\.|[^"\\])*")/g,
            numbers: /\b\d+(\.\d+)?\b/g,
            functions: /\b([a-zA-Z_]\w*)(?=\s*\()/g,
            operators: /[+\-*/%=&|^~<>!?:.]/g
        },
        lua: {
            keywords: /\b(and|break|do|else|elseif|end|false|for|function|if|in|local|nil|not|or|repeat|return|then|true|until|while)\b/g,
            types: /\b(print|type|tostring|tonumber|pairs|ipairs|table|string|math|io|os)\b/g,
            comments: /(--\[\[[\s\S]*?\]\]|--.*$)/gm,
            strings: /("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\[\[[\s\S]*?\]\])/g,
            numbers: /\b\d+(\.\d+)?\b/g,
            functions: /\b([a-zA-Z_]\w*)(?=\s*[\({])/g,
            operators: /[+\-*/%=#~<>]/g
        },
        csharp: {
            keywords: /\b(abstract|as|base|bool|break|byte|case|catch|char|checked|class|const|continue|decimal|default|delegate|do|double|else|enum|event|explicit|extern|false|finally|fixed|float|for|foreach|goto|if|implicit|in|int|interface|internal|is|lock|long|namespace|new|null|object|operator|out|override|params|private|protected|public|readonly|ref|return|sbyte|sealed|short|sizeof|stackalloc|static|string|struct|switch|this|throw|true|try|typeof|uint|ulong|unchecked|unsafe|ushort|using|virtual|void|volatile|while|async|await|var)\b/g,
            types: /\b(String|Int32|Int64|Boolean|Object|List|Dictionary|Task|Action|Func)\b/g,
            comments: /(\/\*[\s\S]*?\*\/|\/\/.*$)/gm,
            strings: /(@"(?:""|[^"])*"|\$"(?:\\.|[^"\\])*"|"(?:\\.|[^"\\])*")/g,
            numbers: /\b\d+(\.\d+)?([mMfFdDlL])?\b/g,
            functions: /\b([a-zA-Z_]\w*)(?=\s*[<(])/g,
            operators: /[+\-*/%=&|^~<>!?:.]/g
        },
        html: {
            keywords: /<\/?([a-zA-Z0-9\-]+)/g,
            types: /\b([a-zA-Z\-]+)(?=\=)/g,
            comments: /(<!--[\s\S]*?-->)/g,
            strings: /("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')/g,
            numbers: /\b\d+(\.\d+)?(px|em|rem|%|vh|vw)?\b/g,
            functions: /&[a-zA-Z0-9#]+;/g,
            operators: /[<>\/="]/g
        },
        css: {
            keywords: /@(?:import|media|keyframes|supports|font-face|charset)/g,
            types: /\b(color|background|margin|padding|border|display|flex|grid|position|top|left|width|height|font-size|font-family)\b/g,
            comments: /(\/\*[\s\S]*?\*\/)/g,
            strings: /("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')/g,
            numbers: /\b\d+(\.\d+)?(px|em|rem|%|vh|vw|s|ms|deg)?\b/g,
            functions: /\b([a-zA-Z\-]+)(?=\()/g,
            operators: /[{}:;,]/g
        },
        json: {
            keywords: /\b(true|false|null)\b/g,
            types: /"([^"\\]|\\.)*"(?=\s*:)/g,
            comments: /(\/\/.*$)/gm,
            strings: /"(?:\\.|[^"\\])*"/g,
            numbers: /-?\b\d+(\.\d+)?([eE][+-]?\d+)?\b/g,
            functions: /[{}[\]]/g,
            operators: /[:,]/g
        }
    };

    function escapeHtml(text) {
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    function renderGutter(lineCount) {
        let html = '';
        const cursorLine = getCursorLine();
        for (let i = 1; i <= lineCount; i++) {
            const activeClass = (i === cursorLine) ? ' active' : '';
            html += `<div class="gutter-line${activeClass}">${i}</div>`;
        }
        gutter.innerHTML = html;
    }

    function getCursorLine() {
        const text = codeEditor.value.substring(0, codeEditor.selectionStart);
        return text.split('\n').length;
    }

    function getCursorCol() {
        const text = codeEditor.value.substring(0, codeEditor.selectionStart);
        const lastNewline = text.lastIndexOf('\n');
        return (lastNewline === -1) ? text.length + 1 : text.length - lastNewline;
    }

    function highlightCode(text, lang) {
        const rules = LANGUAGE_RULES[lang];
        if (!rules) {
            return escapeHtml(text);
        }

        // Token extraction with placeholder mask to prevent duplicate replacements
        const tokens = [];
        let masked = text;

        function extractTokens(regex, className) {
            masked = masked.replace(regex, function(match) {
                const idx = tokens.length;
                tokens.push({ text: match, className: className });
                return `___CF_TOK_${idx}___`;
            });
        }

        // Order matters: comments and strings first so code keywords inside them are not touched
        if (rules.comments) extractTokens(rules.comments, 'tok-comment');
        if (rules.strings) extractTokens(rules.strings, 'tok-string');
        if (rules.keywords) extractTokens(rules.keywords, 'tok-keyword');
        if (rules.types) extractTokens(rules.types, 'tok-type');
        if (rules.functions) extractTokens(rules.functions, 'tok-function');
        if (rules.numbers) extractTokens(rules.numbers, 'tok-number');

        let escaped = escapeHtml(masked);

        // Put tokens back with syntax class tags
        escaped = escaped.replace(/___CF_TOK_(\d+)___/g, function(_, idx) {
            const tok = tokens[parseInt(idx, 10)];
            if (!tok) return '';
            return `<span class="${tok.className}">${escapeHtml(tok.text)}</span>`;
        });

        // Apply error diagnostics underlines
        if (diagnostics && diagnostics.length > 0) {
            const lines = escaped.split('\n');
            diagnostics.forEach(d => {
                const lineIdx = d.line - 1;
                if (lineIdx >= 0 && lineIdx < lines.length) {
                    const sevClass = (d.severity === 'error') ? 'diag-error' : 'diag-warning';
                    lines[lineIdx] = `<span class="${sevClass}" title="${escapeHtml(d.message || '')}">${lines[lineIdx]}</span>`;
                }
            });
            escaped = lines.join('\n');
        }

        return escaped;
    }

    function updateEditor(markDirty = false) {
        const content = codeEditor.value;
        const lines = content.split('\n');
        renderGutter(lines.length);

        const highlighted = highlightCode(content, currentLanguage);
        highlightLayer.innerHTML = highlighted + '\n';

        if (!isApplyingHistory && markDirty) {
            undoStack.push(content);
            if (undoStack.length > 100) undoStack.shift();
            redoStack = [];
        }

        if (markDirty && window.AndroidBridge && window.AndroidBridge.onContentChanged) {
            window.AndroidBridge.onContentChanged(content, true);
        }
    }

    function notifyCursorPosition() {
        const line = getCursorLine();
        const col = getCursorCol();
        const lines = codeEditor.value.split('\n');
        renderGutter(lines.length);

        if (window.AndroidBridge && window.AndroidBridge.onCursorChanged) {
            window.AndroidBridge.onCursorChanged(line, col);
        }
    }

    // Bracket auto-closing map
    const BRACKET_MAP = {
        '(': ')',
        '[': ']',
        '{': '}',
        '"': '"',
        "'": "'",
        '`': '`'
    };

    // Keyboard and Touch Event Handlers
    codeEditor.addEventListener('keydown', function(e) {
        const start = this.selectionStart;
        const end = this.selectionEnd;
        const val = this.value;

        // Tab Key: insert spaces
        if (e.key === 'Tab') {
            e.preventDefault();
            const spaces = ' '.repeat(tabSize);
            this.value = val.substring(0, start) + spaces + val.substring(end);
            this.selectionStart = this.selectionEnd = start + tabSize;
            updateEditor();
            return;
        }

        // Enter Key: Auto-indentation
        if (e.key === 'Enter') {
            e.preventDefault();
            const lineStart = val.lastIndexOf('\n', start - 1) + 1;
            const currentLine = val.substring(lineStart, start);
            const matchIndent = currentLine.match(/^\s*/);
            let indent = matchIndent ? matchIndent[0] : '';

            // If line ends with opening bracket, increase indent
            const trimmed = currentLine.trim();
            if (trimmed.endsWith('{') || trimmed.endsWith(':') || trimmed.endsWith('(') || trimmed.endsWith('[')) {
                indent += ' '.repeat(tabSize);
            }

            const insertText = '\n' + indent;
            this.value = val.substring(0, start) + insertText + val.substring(end);
            this.selectionStart = this.selectionEnd = start + insertText.length;
            updateEditor();
            return;
        }

        // Bracket auto-close
        if (BRACKET_MAP[e.key] && start === end) {
            const closer = BRACKET_MAP[e.key];
            e.preventDefault();
            this.value = val.substring(0, start) + e.key + closer + val.substring(end);
            this.selectionStart = this.selectionEnd = start + 1;
            updateEditor();
            return;
        }
    });

    const autocompleteBox = document.getElementById('autocomplete-box');
    const AUTOCOMPLETE_DATA = {
        html: [
            { text: '<!DOCTYPE html>\n<html lang="en">\n<head>\n    <meta charset="UTF-8">\n    <title>Document</title>\n</head>\n<body>\n    \n</body>\n</html>', label: '!html5', desc: 'template' },
            { text: '<div>\n    \n</div>', label: 'div', desc: 'tag' },
            { text: '<p>\n    \n</p>', label: 'p', desc: 'tag' },
            { text: '<span></span>', label: 'span', desc: 'tag' },
            { text: '<h1></h1>', label: 'h1', desc: 'tag' },
            { text: '<h2></h2>', label: 'h2', desc: 'tag' },
            { text: '<h3></h3>', label: 'h3', desc: 'tag' },
            { text: '<button type="button"></button>', label: 'button', desc: 'tag' },
            { text: '<input type="text" placeholder="" />', label: 'input', desc: 'tag' },
            { text: '<form action="" method="post">\n    \n</form>', label: 'form', desc: 'tag' },
            { text: '<a href=""></a>', label: 'a', desc: 'tag' },
            { text: '<img src="" alt="" />', label: 'img', desc: 'tag' },
            { text: '<ul id="">\n    <li></li>\n</ul>', label: 'ul', desc: 'tag' },
            { text: '<li></li>', label: 'li', desc: 'tag' },
            { text: '<table border="1">\n    <tr><td></td></tr>\n</table>', label: 'table', desc: 'tag' },
            { text: '<header>\n    \n</header>', label: 'header', desc: 'tag' },
            { text: '<footer>\n    \n</footer>', label: 'footer', desc: 'tag' },
            { text: '<nav>\n    \n</nav>', label: 'nav', desc: 'tag' },
            { text: '<section>\n    \n</section>', label: 'section', desc: 'tag' },
            { text: '<link rel="stylesheet" href="style.css">', label: 'link:css', desc: 'tag' },
            { text: '<script src="app.js"></script>', label: 'script:src', desc: 'tag' },
            { text: '<script>\n    \n</script>', label: 'script', desc: 'tag' },
            { text: '<style>\n    \n</style>', label: 'style', desc: 'tag' },
            { text: '<meta charset="UTF-8">', label: 'meta', desc: 'tag' }
        ],
        css: [
            { text: 'color: #58A6FF;', label: 'color', desc: 'prop' },
            { text: 'background: #0F141E;', label: 'background', desc: 'prop' },
            { text: 'background-color: transparent;', label: 'background-color', desc: 'prop' },
            { text: 'font-size: 14px;', label: 'font-size', desc: 'prop' },
            { text: 'font-family: sans-serif;', label: 'font-family', desc: 'prop' },
            { text: 'font-weight: bold;', label: 'font-weight', desc: 'prop' },
            { text: 'padding: 8px 12px;', label: 'padding', desc: 'prop' },
            { text: 'margin: 0 auto;', label: 'margin', desc: 'prop' },
            { text: 'display: flex;', label: 'display:flex', desc: 'prop' },
            { text: 'display: grid;', label: 'display:grid', desc: 'prop' },
            { text: 'flex-direction: column;', label: 'flex-direction', desc: 'prop' },
            { text: 'justify-content: center;', label: 'justify-content', desc: 'prop' },
            { text: 'align-items: center;', label: 'align-items', desc: 'prop' },
            { text: 'border: 1px solid #30363D;', label: 'border', desc: 'prop' },
            { text: 'border-radius: 8px;', label: 'border-radius', desc: 'prop' },
            { text: 'width: 100%;', label: 'width', desc: 'prop' },
            { text: 'height: 100%;', label: 'height', desc: 'prop' },
            { text: 'box-shadow: 0 4px 12px rgba(0,0,0,0.4);', label: 'box-shadow', desc: 'prop' },
            { text: 'cursor: pointer;', label: 'cursor', desc: 'prop' }
        ],
        javascript: [
            { text: 'console.log();', label: 'console.log', desc: 'builtin' },
            { text: 'function myFunc() {\n    \n}', label: 'function', desc: 'fn' },
            { text: 'const name = "";', label: 'const', desc: 'keyword' },
            { text: 'let count = 0;', label: 'let', desc: 'keyword' },
            { text: 'document.getElementById("")', label: 'getElementById', desc: 'dom' },
            { text: 'document.querySelector("")', label: 'querySelector', desc: 'dom' },
            { text: 'addEventListener("click", (e) => {\n    \n});', label: 'addEventListener', desc: 'event' },
            { text: 'if (condition) {\n    \n}', label: 'if', desc: 'keyword' },
            { text: 'for (let i = 0; i < 10; i++) {\n    \n}', label: 'for', desc: 'loop' },
            { text: 'setTimeout(() => {\n    \n}, 1000);', label: 'setTimeout', desc: 'async' }
        ],
        python: [
            { text: 'def function_name():\n    pass', label: 'def', desc: 'fn' },
            { text: 'class MyClass:\n    def __init__(self):\n        pass', label: 'class', desc: 'class' },
            { text: 'import sys', label: 'import', desc: 'keyword' },
            { text: 'from math import pi', label: 'from', desc: 'keyword' },
            { text: 'print("Hello, World!")', label: 'print', desc: 'builtin' },
            { text: 'if condition:\n    pass\nelif other:\n    pass\nelse:\n    pass', label: 'if', desc: 'control' },
            { text: 'for item in items:\n    pass', label: 'for', desc: 'loop' },
            { text: 'while True:\n    break', label: 'while', desc: 'loop' },
            { text: 'if __name__ == "__main__":\n    main()', label: 'main', desc: 'snippet' }
        ],
        kotlin: [
            { text: 'fun main() {\n    println("Hello from Kotlin!")\n}', label: 'main', desc: 'snippet' },
            { text: 'fun myMethod(): Unit {\n    \n}', label: 'fun', desc: 'fn' },
            { text: 'val name: String = ""', label: 'val', desc: 'keyword' },
            { text: 'var count: Int = 0', label: 'var', desc: 'keyword' },
            { text: 'class MyClass(val id: Int) {\n    \n}', label: 'class', desc: 'class' },
            { text: 'data class User(val id: String, val name: String)', label: 'data class', desc: 'class' },
            { text: 'println()', label: 'println', desc: 'builtin' }
        ],
        rust: [
            { text: 'fn main() {\n    println!("Hello from Rust!");\n}', label: 'main', desc: 'snippet' },
            { text: 'fn my_func() {\n    \n}', label: 'fn', desc: 'fn' },
            { text: 'let mut count = 0;', label: 'let mut', desc: 'keyword' },
            { text: 'struct MyStruct {\n    field: String,\n}', label: 'struct', desc: 'type' },
            { text: 'println!("Hello, {}!", name);', label: 'println!', desc: 'macro' }
        ],
        cpp: [
            { text: '#include <iostream>\n\nint main() {\n    std::cout << "Hello C++!" << std::endl;\n    return 0;\n}', label: 'main', desc: 'snippet' },
            { text: 'std::cout <<  << std::endl;', label: 'cout', desc: 'stream' },
            { text: 'std::string ', label: 'string', desc: 'type' },
            { text: 'std::vector<int> ', label: 'vector', desc: 'stl' }
        ],
        go: [
            { text: 'package main\n\nimport "fmt"\n\nfunc main() {\n    fmt.Println("Hello Go!")\n}', label: 'main', desc: 'snippet' },
            { text: 'func myFunc() {\n    \n}', label: 'func', desc: 'fn' },
            { text: 'fmt.Println()', label: 'Println', desc: 'fmt' }
        ]
    };

    function checkAutocomplete() {
        if (!autocompleteBox) return;
        const cursor = codeEditor.selectionStart || 0;
        const text = codeEditor.value || '';
        const lineStart = text.lastIndexOf('\n', cursor - 1) + 1;
        const linePrefix = text.substring(lineStart, cursor);
        const match = linePrefix.match(/([<!a-zA-Z0-9_\-:]+)$/);
        if (!match || match[1].length < 1) {
            autocompleteBox.style.display = 'none';
            return;
        }

        const rawPrefix = match[1].toLowerCase();
        const prefix = rawPrefix.startsWith('<') ? rawPrefix.substring(1) : rawPrefix;
        const lang = currentLanguage.toLowerCase();
        let dict = AUTOCOMPLETE_DATA[lang] || [];
        if (dict.length === 0) {
            dict = (AUTOCOMPLETE_DATA['javascript'] || []).concat(AUTOCOMPLETE_DATA['python'] || []);
        }
        const suggestions = dict.filter(it => it.label.toLowerCase().startsWith(prefix) || it.label.toLowerCase().startsWith(rawPrefix) || it.label.toLowerCase().includes(prefix));

        if (suggestions.length === 0) {
            autocompleteBox.style.display = 'none';
            return;
        }

        autocompleteBox.innerHTML = '';
        suggestions.slice(0, 6).forEach(item => {
            const row = document.createElement('div');
            row.className = 'ac-item';
            row.innerHTML = '<span>' + item.label + '</span><span class="ac-type">' + item.desc + '</span>';
            row.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                applyAutocomplete(item.text, match[1]);
            });
            autocompleteBox.appendChild(row);
        });

        const lineIndex = text.substring(0, cursor).split('\n').length;
        const topPos = Math.max(10, (lineIndex * 20) - codeEditor.scrollTop + 22);
        autocompleteBox.style.top = topPos + 'px';
        autocompleteBox.style.left = '45px';
        autocompleteBox.style.display = 'block';
    }

    function applyAutocomplete(snippet, replacePrefix) {
        const cursor = codeEditor.selectionStart || 0;
        const text = codeEditor.value || '';
        const start = cursor - replacePrefix.length;
        codeEditor.value = text.substring(0, start) + snippet + text.substring(cursor);
        codeEditor.selectionStart = codeEditor.selectionEnd = start + snippet.length;
        if (autocompleteBox) autocompleteBox.style.display = 'none';
        codeEditor.focus();
        updateEditor(true);
        notifyCursorPosition();
    }

    codeEditor.addEventListener('input', function() {
        updateEditor(true);
        checkAutocomplete();
    });

    codeEditor.addEventListener('click', function() {
        notifyCursorPosition();
        if (autocompleteBox) autocompleteBox.style.display = 'none';
    });
    codeEditor.addEventListener('keyup', notifyCursorPosition);

    // Sync scroll between textarea and highlight layer
    codeEditor.addEventListener('scroll', function() {
        highlightLayer.scrollTop = codeEditor.scrollTop;
        highlightLayer.scrollLeft = codeEditor.scrollLeft;
        gutter.scrollTop = codeEditor.scrollTop;
        if (autocompleteBox) autocompleteBox.style.display = 'none';
    });

    // Public API exposed to Android WebView
    window.CodeForgeEditor = {
        setContent: function(content, language) {
            codeEditor.value = content || '';
            if (language) currentLanguage = language.toLowerCase();
            undoStack = [codeEditor.value];
            redoStack = [];
            updateEditor(false);
            notifyCursorPosition();
        },
        getContent: function() {
            return codeEditor.value;
        },
        setLanguage: function(lang) {
            currentLanguage = (lang || 'plaintext').toLowerCase();
            updateEditor(false);
        },
        setFontSize: function(sp) {
            const size = sp || 14;
            codeEditor.style.fontSize = size + 'px';
            highlightLayer.style.fontSize = size + 'px';
            gutter.style.fontSize = size + 'px';
            updateEditor();
        },
        setTabWidth: function(width) {
            tabSize = width || 4;
            codeEditor.style.tabSize = tabSize;
            highlightLayer.style.tabSize = tabSize;
        },
        setWordWrap: function(enabled) {
            wordWrap = !!enabled;
            const wrapVal = wordWrap ? 'pre-wrap' : 'pre';
            codeEditor.style.whiteSpace = wrapVal;
            highlightLayer.style.whiteSpace = wrapVal;
        },
        gotoLine: function(lineNum) {
            const lines = codeEditor.value.split('\n');
            const targetLine = Math.max(1, Math.min(lineNum, lines.length));
            let charIndex = 0;
            for (let i = 0; i < targetLine - 1; i++) {
                charIndex += lines[i].length + 1;
            }
            codeEditor.focus();
            codeEditor.setSelectionRange(charIndex, charIndex);
            notifyCursorPosition();

            // Scroll into view
            const lineHeight = 21;
            codeEditor.scrollTop = (targetLine - 1) * lineHeight;
        },
        undo: function() {
            if (undoStack.length > 1) {
                isApplyingHistory = true;
                const current = undoStack.pop();
                redoStack.push(current);
                const prev = undoStack[undoStack.length - 1];
                codeEditor.value = prev;
                updateEditor();
                notifyCursorPosition();
                isApplyingHistory = false;
            }
        },
        redo: function() {
            if (redoStack.length > 0) {
                isApplyingHistory = true;
                const next = redoStack.pop();
                undoStack.push(next);
                codeEditor.value = next;
                updateEditor();
                notifyCursorPosition();
                isApplyingHistory = false;
            }
        },
        search: function(query, replaceWith, replaceAll) {
            if (!query) return 0;
            const content = codeEditor.value;
            if (replaceWith !== undefined && replaceWith !== null) {
                if (replaceAll) {
                    codeEditor.value = content.replaceAll(query, replaceWith);
                } else {
                    const idx = content.indexOf(query, codeEditor.selectionStart);
                    if (idx !== -1) {
                        codeEditor.value = content.substring(0, idx) + replaceWith + content.substring(idx + query.length);
                        codeEditor.setSelectionRange(idx, idx + replaceWith.length);
                    }
                }
                updateEditor();
            } else {
                // Find next
                let startPos = codeEditor.selectionEnd;
                let idx = content.indexOf(query, startPos);
                if (idx === -1) {
                    idx = content.indexOf(query, 0); // wrap around
                }
                if (idx !== -1) {
                    codeEditor.focus();
                    codeEditor.setSelectionRange(idx, idx + query.length);
                    notifyCursorPosition();
                    return 1;
                }
            }
            return 0;
        },
        setDiagnostics: function(diagJson) {
            try {
                diagnostics = typeof diagJson === 'string' ? JSON.parse(diagJson) : diagJson;
                updateEditor();
            } catch(e) {
                console.error("Failed to parse diagnostics", e);
            }
        },
        insertText: function(text) {
            const start = codeEditor.selectionStart || 0;
            const end = codeEditor.selectionEnd || 0;
            val = codeEditor.value || '';
            codeEditor.value = val.substring(0, start) + text + val.substring(end);
            codeEditor.selectionStart = codeEditor.selectionEnd = start + text.length;
            codeEditor.focus();
            updateEditor(true);
            notifyCursorPosition();
        },
        formatDocument: function() {
            const val = codeEditor.value;
            if (!val) return;
            const lines = val.split('\n');
            let indent = 0;
            const formatted = lines.map(line => {
                let trimmed = line.trim();
                if (trimmed.startsWith('}') || trimmed.startsWith('</') || trimmed.startsWith(']')) {
                    indent = Math.max(0, indent - 1);
                }
                const res = ' '.repeat(indent * tabSize) + trimmed;
                if ((trimmed.endsWith('{') || trimmed.match(/<[a-zA-Z0-9]+[^>/]*>$/) || trimmed.endsWith('[')) && !trimmed.includes('</')) {
                    indent++;
                }
                return res;
            }).join('\n');
            codeEditor.value = formatted;
            updateEditor(true);
            notifyCursorPosition();
        }
    };

    // Notify Android container that editor is loaded and ready
    document.addEventListener('DOMContentLoaded', function() {
        updateEditor(false);
        if (window.AndroidBridge && window.AndroidBridge.onEditorReady) {
            window.AndroidBridge.onEditorReady();
        }
    });

})();
