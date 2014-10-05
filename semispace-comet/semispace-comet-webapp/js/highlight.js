
var hljs = new function() {
    var LANGUAGES = {};
    var selected_languages = {};

    function escape(value) {
        return value.replace(/&/gm, '&amp;').replace(/</gm, '&lt;').replace(/>/gm, '&gt;');
    }

    function contains(array, item) {
        if (!array) {
            return false;
        }
        for (var i = 0; i < array.length; i++) {
            if (array[i] == item) {
                return true;
            }
        }
        return false;
    }

    function highlight(language_name, value) {
        function compileSubModes(mode, language) {
            mode.sub_modes = [];
            for (var i = 0; i < mode.contains.length; i++) {
                for (var j = 0; j < language.modes.length; j++) {
                    if (language.modes[j].className == mode.contains[i]) {
                        mode.sub_modes[mode.sub_modes.length] = language.modes[j];
                    }
                }
            }
        }

        function subMode(lexem, mode) {
            if (!mode.contains) {
                return null;
            }
            if (!mode.sub_modes) {
                compileSubModes(mode, language);
            }
            for (var i = 0; i < mode.sub_modes.length; i++) {
                if (mode.sub_modes[i].beginRe.test(lexem)) {
                    return mode.sub_modes[i];
                }
            }
            return null;
        }

        function endOfMode(mode_index, lexem) {
            if (modes[mode_index].end && modes[mode_index].endRe.test(lexem)) {
                return 1;
            }
            if (modes[mode_index].endsWithParent) {
                var level = endOfMode(mode_index - 1, lexem);
                return level ? level + 1 : 0;
            }
            return 0;
        }

        function isIllegal(lexem, mode) {
            return mode.illegalRe && mode.illegalRe.test(lexem);
        }

        function compileTerminators(mode, language) {
            var terminators = [];
            var i;

            function addTerminator(re) {
                if (!contains(terminators, re)) {
                    terminators[terminators.length] = re;
                }
            }

            if (mode.contains) {
                for (i = 0; i < language.modes.length; i++) {
                    if (contains(mode.contains, language.modes[i].className)) {
                        addTerminator(language.modes[i].begin);
                    }
                }
            }

            var index = modes.length - 1;
            do {
                if (modes[index].end) {
                    addTerminator(modes[index].end);
                }
                index--;
            } while (modes[index + 1].endsWithParent);

            if (mode.illegal) {
                addTerminator(mode.illegal);
            }

            var terminator_re = '(' + terminators[0];
            for (i = 0; i < terminators.length; i++) {
                terminator_re += '|' + terminators[i];
            }
            terminator_re += ')';
            return langRe(language, terminator_re);
        }

        function eatModeChunk(value, index) {
            var mode = modes[modes.length - 1];
            if (!mode.terminators) {
                mode.terminators = compileTerminators(mode, language);
            }
            value = value.substr(index);
            var match = mode.terminators.exec(value);
            if (!match) {
                return [value, '', true];
            }
            if (match.index === 0) {
                return ['', match[0], false];
            } else {
                return [value.substr(0, match.index), match[0], false];
            }
        }

        function keywordMatch(mode, match) {
            var match_str = language.case_insensitive ? match[0].toLowerCase() : match[0];
            for (var className in mode.keywordGroups) {
                if (!mode.keywordGroups.hasOwnProperty(className)) {
                    continue;
                }
                var value = mode.keywordGroups[className].hasOwnProperty(match_str);
                if (value) {
                    return [className, value];
                }
            }
            return false;
        }

        function processKeywords(buffer, mode) {
            if (!mode.keywords || !mode.lexems) {
                return escape(buffer);
            }
            if (!mode.lexemsRe) {
                var lexems_re = '(' + mode.lexems[0];
                for (var i = 1; i < mode.lexems.length; i++) {
                    lexems_re += '|' + mode.lexems[i];
                }
                lexems_re += ')';
                mode.lexemsRe = langRe(language, lexems_re, true);
            }
            var result = '';
            var last_index = 0;
            mode.lexemsRe.lastIndex = 0;
            var match = mode.lexemsRe.exec(buffer);
            while (match) {
                result += escape(buffer.substr(last_index, match.index - last_index));
                var keyword_match = keywordMatch(mode, match);
                if (keyword_match) {
                    keyword_count += keyword_match[1];
                    result += '<span class="' + keyword_match[0] + '">' + escape(match[0]) + '</span>';
                } else {
                    result += escape(match[0]);
                }
                last_index = mode.lexemsRe.lastIndex;
                match = mode.lexemsRe.exec(buffer);
            }
            result += escape(buffer.substr(last_index, buffer.length - last_index));
            return result;
        }

        function processBuffer(buffer, mode) {
            if (mode.subLanguage && selected_languages[mode.subLanguage]) {
                var result = highlight(mode.subLanguage, buffer);
                keyword_count += result.keyword_count;
                relevance += result.relevance;
                return result.value;
            } else {
                return processKeywords(buffer, mode);
            }
        }

        function startNewMode(mode, lexem) {
            var markup = mode.noMarkup ? '' : '<span class="' + mode.className + '">';
            if (mode.returnBegin) {
                result += markup;
                mode.buffer = '';
            } else if (mode.excludeBegin) {
                result += escape(lexem) + markup;
                mode.buffer = '';
            } else {
                result += markup;
                mode.buffer = lexem;
            }
            modes[modes.length] = mode;
        }

        function processModeInfo(buffer, lexem, end) {
            var current_mode = modes[modes.length - 1];
            if (end) {
                result += processBuffer(current_mode.buffer + buffer, current_mode);
                return false;
            }

            var new_mode = subMode(lexem, current_mode);
            if (new_mode) {
                result += processBuffer(current_mode.buffer + buffer, current_mode);
                startNewMode(new_mode, lexem);
                relevance += new_mode.relevance;
                return new_mode.returnBegin;
            }

            var end_level = endOfMode(modes.length - 1, lexem);
            if (end_level) {
                var markup = current_mode.noMarkup ? '' : '</span>';
                if (current_mode.returnEnd) {
                    result += processBuffer(current_mode.buffer + buffer, current_mode) + markup;
                } else if (current_mode.excludeEnd) {
                    result += processBuffer(current_mode.buffer + buffer, current_mode) + markup + escape(lexem);
                } else {
                    result += processBuffer(current_mode.buffer + buffer + lexem, current_mode) + markup;
                }
                while (end_level > 1) {
                    markup = modes[modes.length - 2].noMarkup ? '' : '</span>';
                    result += markup;
                    end_level--;
                    modes.length--;
                }
                modes.length--;
                modes[modes.length - 1].buffer = '';
                if (current_mode.starts) {
                    for (var i = 0; i < language.modes.length; i++) {
                        if (language.modes[i].className == current_mode.starts) {
                            startNewMode(language.modes[i], '');
                            break;
                        }
                    }
                }
                return current_mode.returnEnd;
            }

            if (isIllegal(lexem, current_mode)) {
                throw 'Illegal';
            }
            return null;
        }

        var language = LANGUAGES[language_name];
        var modes = [language.defaultMode];
        var relevance = 0;
        var keyword_count = 0;
        var result = '';
        try {
            var index = 0;
            language.defaultMode.buffer = '';
            do {
                var mode_info = eatModeChunk(value, index);
                var return_lexem = processModeInfo(mode_info[0], mode_info[1], mode_info[2]);
                index += mode_info[0].length;
                if (!return_lexem) {
                    index += mode_info[1].length;
                }
            } while (!mode_info[2]);
            if (modes.length > 1) {
                throw 'Illegal';
            }
            return {
                relevance: relevance,
                keyword_count: keyword_count,
                value: result
            };
        } catch (e) {
            if (e == 'Illegal') {
                return {
                    relevance: 0,
                    keyword_count: 0,
                    value: escape(value)
                };
            } else {
                throw e;
            }
        }
    }

    function blockText(block) {
        var result = '';
        for (var i = 0; i < block.childNodes.length; i++) {
            if (block.childNodes[i].nodeType == 3) {
                result += block.childNodes[i].nodeValue;
            } else if (block.childNodes[i].nodeName == 'BR') {
                result += '\n';
            } else {
                throw 'No highlight';
            }
        }
        return result;
    }

    function blockLanguage(block) {
        var classes = block.className.split(/\s+/);
        for (var i = 0; i < classes.length; i++) {
            if (classes[i] == 'no-highlight') {
                throw 'No highlight';
            }
            if (LANGUAGES[classes[i]]) {
                return classes[i];
            }
        }
        return null;
    }

    function highlightBlock(block, tabReplace) {
        var result;
        try {
            var text = blockText(block);
            var language = blockLanguage(block);
        } catch (e) {
            if (e == 'No highlight') {
                return;
            }
        }

        if (language) {
            result = highlight(language, text).value;
        } else {
            var max_relevance = 0;
            for (var key in selected_languages) {
                if (!selected_languages.hasOwnProperty(key)) {
                    continue;
                }
                var lang_result = highlight(key, text);
                var relevance = lang_result.keyword_count + lang_result.relevance;
                if (relevance > max_relevance) {
                    max_relevance = relevance;
                    result = lang_result.value;
                    language = key;
                }
            }
        }

        if (result) {
            if (tabReplace) {
                result = result.replace(/^(\t+)/gm, function(p1) {
                    return p1.replace(/\t/g, tabReplace);
                });
            }
            var class_name = block.className;
            if (!class_name.match(language)) {
                class_name += ' ' + language;
            }
            // See these 4 lines? This is IE's notion of "block.innerHTML = result". Love this browser :-/
            var container = document.createElement('div');
            container.innerHTML = '<div class="source"><pre class="' + class_name + '">' + result + '</pre></div>';
            var environment = block.parentNode.parentNode;
            environment.replaceChild(container.firstChild, block.parentNode);
        }
    }

    function langRe(language, value, global) {
        var mode = 'm' + (language.case_insensitive ? 'i' : '') + (global ? 'g' : '');
        return new RegExp(value, mode);
    }

    function compileModes() {
        for (var i in LANGUAGES) {
            if (!LANGUAGES.hasOwnProperty(i)) {
                continue;
            }
            var language = LANGUAGES[i];
            for (var j = 0; j < language.modes.length; j++) {
                if (language.modes[j].begin) {
                    language.modes[j].beginRe = langRe(language, '^' + language.modes[j].begin);
                }
                if (language.modes[j].end) {
                    language.modes[j].endRe = langRe(language, '^' + language.modes[j].end);
                }
                if (language.modes[j].illegal) {
                    language.modes[j].illegalRe = langRe(language, '^(?:' + language.modes[j].illegal + ')');
                }
                language.defaultMode.illegalRe = langRe(language, '^(?:' + language.defaultMode.illegal + ')');
                if (language.modes[j].relevance === undefined) {
                    language.modes[j].relevance = 1;
                }
            }
        }
    }

    function compileKeywords() {

        function compileModeKeywords(mode) {
            if (!mode.keywordGroups) {
                for (var key in mode.keywords) {
                    if (!mode.keywords.hasOwnProperty(key)) {
                        continue;
                    }
                    if (mode.keywords[key] instanceof Object) {
                        mode.keywordGroups = mode.keywords;
                    } else {
                        mode.keywordGroups = {'keyword': mode.keywords};
                    }
                    break;
                }
            }
        }

        for (var i in LANGUAGES) {
            if (!LANGUAGES.hasOwnProperty(i)) {
                continue;
            }
            var language = LANGUAGES[i];
            compileModeKeywords(language.defaultMode);
            for (var j = 0; j < language.modes.length; j++) {
                compileModeKeywords(language.modes[j]);
            }
        }
    }

    function findCode(divobj) {
        for (var i = 0; i < divobj.childNodes.length; i++) {
            var node = divobj.childNodes[i];
            if (node.nodeName == 'PRE') {
                return node;
            }
            if (!(node.nodeType == 3 && node.nodeValue.match(/\s+/))) {
                return null;
            }
        }
        return null;
    }

    function initHighlighting() {
        if (initHighlighting.called) {
            return;
        }
        initHighlighting.called = true;
        compileModes();
        compileKeywords();
        var i;
        if (arguments.length) {
            for (i = 0; i < arguments.length; i++) {
                if (LANGUAGES[arguments[i]]) {
                    selected_languages[arguments[i]] = LANGUAGES[arguments[i]];
                }
            }
        } else {
            selected_languages = LANGUAGES;
        }
        var divs = document.getElementsByTagName('div');
        for (i = 0; i < divs.length; i++) {
            var class_name = divs[i].className;
            if (class_name.match(RegExp('source'))) {
                var code = findCode(divs[i]);
                if (code) {
                    highlightBlock(code, hljs.tabReplace);
                }
            }
        }
    }

    function initHighlightingOnLoad() {
        var original_arguments = arguments;
        var handler = function() {
            initHighlighting.apply(null, original_arguments);
        };
        if (window.addEventListener) {
            window.addEventListener('DOMContentLoaded', handler, false);
            window.addEventListener('load', handler, false);
        } else if (window.attachEvent) {
            window.attachEvent('onload', handler);
        } else {
            window.onload = handler;
        }
    }

    this.LANGUAGES = LANGUAGES;
    this.initHighlightingOnLoad = initHighlightingOnLoad;
    this.highlightBlock = highlightBlock;
    this.initHighlighting = initHighlighting;

    // Common regexps
    this.IDENT_RE = '[a-zA-Z][a-zA-Z0-9_]*';
    this.UNDERSCORE_IDENT_RE = '[a-zA-Z_][a-zA-Z0-9_]*';
    this.NUMBER_RE = '\\b\\d+(\\.\\d+)?';
    this.C_NUMBER_RE = '\\b(0x[A-Za-z0-9]+|\\d+(\\.\\d+)?)';
    this.RE_STARTERS_RE =
    '!|!=|!==|%|%=|&|&&|&=|\\*|\\*=|\\+|\\+=|,|\\.|-|-=|/|/=|:|;|<|<<|<<=|<=|=|==|===|>|>=|>>|>>=|>>>|>>>=|\\?|\\[|\\{|\\(|\\^|\\^=|\\||\\|=|\\|\\||~';

    // Common modes
    this.APOS_STRING_MODE = {
        className: 'string',
        begin: '\'', end: '\'',
        illegal: '\\n',
        contains: ['escape'],
        relevance: 0
    };
    this.QUOTE_STRING_MODE = {
        className: 'string',
        begin: '"', end: '"',
        illegal: '\\n',
        contains: ['escape'],
        relevance: 0
    };
    this.BACKSLASH_ESCAPE = {
        className: 'escape',
        begin: '\\\\.', end: '^', noMarkup: true,
        relevance: 0
    };
    this.C_LINE_COMMENT_MODE = {
        className: 'comment',
        begin: '//', end: '$',
        relevance: 0
    };
    this.C_BLOCK_COMMENT_MODE = {
        className: 'comment',
        begin: '/\\*', end: '\\*/'
    };
    this.HASH_COMMENT_MODE = {
        className: 'comment',
        begin: '#', end: '$'
    };
    this.C_NUMBER_MODE = {
        className: 'number',
        begin: this.C_NUMBER_RE, end: '^',
        relevance: 0
    };
    this.tabReplace = null;
}();

var initHighlightingOnLoad = hljs.initHighlightingOnLoad;
/*
Language: C#
Author: Jason Diamond <jason@diamond.name>
*/

hljs.LANGUAGES.cs  = {
  defaultMode: {
    lexems: [hljs.UNDERSCORE_IDENT_RE],
    contains: ['comment', 'string', 'number'],
    keywords: {
        // Normal keywords.
        'abstract': 1, 'as': 1, 'base': 1, 'bool': 1, 'break': 1, 'byte': 1, 'case': 1, 'catch': 1, 'char': 1, 'checked': 1, 'class': 1, 'const': 1, 'continue': 1, 'decimal': 1, 'default': 1, 'delegate': 1, 'do': 1, 'do': 1, 'double': 1, 'else': 1, 'enum': 1, 'event': 1, 'explicit': 1, 'extern': 1, 'false': 1, 'finally': 1, 'fixed': 1, 'float': 1, 'for': 1, 'foreach': 1, 'goto': 1, 'if': 1, 'implicit': 1, 'in': 1, 'int': 1, 'interface': 1, 'internal': 1, 'is': 1, 'lock': 1, 'long': 1, 'namespace': 1, 'new': 1, 'null': 1, 'object': 1, 'operator': 1, 'out': 1, 'override': 1, 'params': 1, 'private': 1, 'protected': 1, 'public': 1, 'readonly': 1, 'ref': 1, 'return': 1, 'sbyte': 1, 'sealed': 1, 'short': 1, 'sizeof': 1, 'stackalloc': 1, 'static': 1, 'string': 1, 'struct': 1, 'switch': 1, 'this': 1, 'throw': 1, 'true': 1, 'try': 1, 'typeof': 1, 'uint': 1, 'ulong': 1, 'unchecked': 1, 'unsafe': 1, 'ushort': 1, 'using': 1, 'virtual': 1, 'volatile': 1, 'void': 1, 'while': 1,
        // Contextual keywords.
        'ascending': 1, 'descending': 1, 'from': 1, 'get': 1, 'group': 1, 'into': 1, 'join': 1, 'let': 1, 'orderby': 1, 'partial': 1, 'select': 1, 'set': 1, 'value': 1, 'var': 1, 'where': 1, 'yield': 1
    }
  },
  modes: [
    {
      className: 'comment',
      begin: '///', end: '$', returnBegin: true,
      contains: ['xmlDocTag']
    },
    {
      className: 'xmlDocTag',
      begin: '///|<!--|-->', end: '^'
    },
    {
      className: 'xmlDocTag',
      begin: '</?', end: '>'
    },
    {
      className: 'string',
      begin: '@"', end: '"',
      contains: ['quoteQuote']
    },
    {
      className: 'quoteQuote',
      begin: '""', end: '^'
    },
    hljs.C_LINE_COMMENT_MODE,
    hljs.C_BLOCK_COMMENT_MODE,
    hljs.APOS_STRING_MODE,
    hljs.QUOTE_STRING_MODE,
    hljs.BACKSLASH_ESCAPE,
    hljs.C_NUMBER_MODE
  ]
};
/*
 Language: Python
 */

hljs.LANGUAGES.python = {
    defaultMode: {
        lexems: [hljs.UNDERSCORE_IDENT_RE],
        illegal: '(</|->)',
        contains: ['comment', 'string', 'function', 'class', 'number', 'decorator'],
        keywords: {
            'keyword': {'and': 1, 'elif': 1, 'is': 1, 'global': 1, 'as': 1, 'in': 1, 'if': 1, 'from': 1, 'raise': 1, 'for': 1, 'except': 1, 'finally': 1, 'print': 1, 'import': 1, 'pass': 1, 'return': 1, 'exec': 1, 'else': 1, 'break': 1, 'not': 1, 'with': 1, 'class': 1, 'assert': 1, 'yield': 1, 'try': 1, 'while': 1, 'continue': 1, 'del': 1, 'or': 1, 'def': 1, 'lambda': 1},
            'built_in': {'None': 1, 'True': 1, 'False': 1, 'Ellipsis': 1, 'NotImplemented': 1}
        }
    },
    modes: [
        {
            className: 'function',
            lexems: [hljs.UNDERSCORE_IDENT_RE],
            begin: '\\bdef ', end: ':',
            illegal: '$',
            keywords: {'def': 1},
            contains: ['title', 'params'],
            relevance: 10
        },
        {
            className: 'class',
            lexems: [hljs.UNDERSCORE_IDENT_RE],
            begin: '\\bclass ', end: ':',
            illegal: '[${]',
            keywords: {'class': 1},
            contains: ['title', 'params',],
            relevance: 10
        },
        {
            className: 'title',
            begin: hljs.UNDERSCORE_IDENT_RE, end: '^'
        },
        {
            className: 'params',
            begin: '\\(', end: '\\)',
            contains: ['string']
        },
        hljs.HASH_COMMENT_MODE,
        hljs.C_NUMBER_MODE,
        {
            className: 'string',
            begin: 'u?r?\'\'\'', end: '\'\'\'',
            relevance: 10
        },
        {
            className: 'string',
            begin: 'u?r?"""', end: '"""',
            relevance: 10
        },
        hljs.APOS_STRING_MODE,
        hljs.QUOTE_STRING_MODE,
        hljs.BACKSLASH_ESCAPE,
        {
            className: 'string',
            begin: '(u|r|ur)\'', end: '\'',
            contains: ['escape'],
            relevance: 10
        },
        {
            className: 'string',
            begin: '(u|r|ur)"', end: '"',
            contains: ['escape'],
            relevance: 10
        },
        {
            className: 'decorator',
            begin: '@', end: '$'
        }
    ]
};
/*
 Language: Perl
 Author: Peter Leonov <gojpeg@yandex.ru>
 */

hljs.LANGUAGES.perl = function() {
    var PERL_DEFAULT_CONTAINS = ['comment', 'string', 'number', 'regexp', 'sub', 'variable', 'operator', 'pod'];
    var PERL_KEYWORDS = {'getpwent': 1, 'getservent': 1, 'quotemeta': 1, 'msgrcv': 1, 'scalar': 1, 'kill': 1, 'dbmclose': 1, 'undef': 1, 'lc': 1, 'ma': 1, 'syswrite': 1, 'tr': 1, 'send': 1, 'umask': 1, 'sysopen': 1, 'shmwrite': 1, 'vec': 1, 'qx': 1, 'utime': 1, 'local': 1, 'oct': 1, 'semctl': 1, 'localtime': 1, 'readpipe': 1, 'do': 1, 'return': 1, 'format': 1, 'read': 1, 'sprintf': 1, 'dbmopen': 1, 'pop': 1, 'getpgrp': 1, 'not': 1, 'getpwnam': 1, 'rewinddir': 1, 'qq': 1, 'fileno': 1, 'qw': 1, 'endprotoent': 1, 'wait': 1, 'sethostent': 1, 'bless': 1, 's': 1, 'opendir': 1, 'continue': 1, 'each': 1, 'sleep': 1, 'endgrent': 1, 'shutdown': 1, 'dump': 1, 'chomp': 1, 'connect': 1, 'getsockname': 1, 'die': 1, 'socketpair': 1, 'close': 1, 'flock': 1, 'exists': 1, 'index': 1, 'shmget': 1, 'sub': 1, 'for': 1, 'endpwent': 1, 'redo': 1, 'lstat': 1, 'msgctl': 1, 'setpgrp': 1, 'abs': 1, 'exit': 1, 'select': 1, 'print': 1, 'ref': 1, 'gethostbyaddr': 1, 'unshift': 1, 'fcntl': 1, 'syscall': 1, 'goto': 1, 'getnetbyaddr': 1, 'join': 1, 'gmtime': 1, 'symlink': 1, 'semget': 1, 'splice': 1, 'x': 1, 'getpeername': 1, 'recv': 1, 'log': 1, 'setsockopt': 1, 'cos': 1, 'last': 1, 'reverse': 1, 'gethostbyname': 1, 'getgrnam': 1, 'study': 1, 'formline': 1, 'endhostent': 1, 'times': 1, 'chop': 1, 'length': 1, 'gethostent': 1, 'getnetent': 1, 'pack': 1, 'getprotoent': 1, 'getservbyname': 1, 'rand': 1, 'mkdir': 1, 'pos': 1, 'chmod': 1, 'y': 1, 'substr': 1, 'endnetent': 1, 'printf': 1, 'next': 1, 'open': 1, 'msgsnd': 1, 'readdir': 1, 'use': 1, 'unlink': 1, 'getsockopt': 1, 'getpriority': 1, 'rindex': 1, 'wantarray': 1, 'hex': 1, 'system': 1, 'getservbyport': 1, 'endservent': 1, 'int': 1, 'chr': 1, 'untie': 1, 'rmdir': 1, 'prototype': 1, 'tell': 1, 'listen': 1, 'fork': 1, 'shmread': 1, 'ucfirst': 1, 'setprotoent': 1, 'else': 1, 'sysseek': 1, 'link': 1, 'getgrgid': 1, 'shmctl': 1, 'waitpid': 1, 'unpack': 1, 'getnetbyname': 1, 'reset': 1, 'chdir': 1, 'grep': 1, 'split': 1, 'require': 1, 'caller': 1, 'lcfirst': 1, 'until': 1, 'warn': 1, 'while': 1, 'values': 1, 'shift': 1, 'telldir': 1, 'getpwuid': 1, 'my': 1, 'getprotobynumber': 1, 'delete': 1, 'and': 1, 'sort': 1, 'uc': 1, 'defined': 1, 'srand': 1, 'accept': 1, 'package': 1, 'seekdir': 1, 'getprotobyname': 1, 'semop': 1, 'our': 1, 'rename': 1, 'seek': 1, 'if': 1, 'q': 1, 'chroot': 1, 'sysread': 1, 'setpwent': 1, 'no': 1, 'crypt': 1, 'getc': 1, 'chown': 1, 'sqrt': 1, 'write': 1, 'setnetent': 1, 'setpriority': 1, 'foreach': 1, 'tie': 1, 'sin': 1, 'msgget': 1, 'map': 1, 'stat': 1, 'getlogin': 1, 'unless': 1, 'elsif': 1, 'truncate': 1, 'exec': 1, 'keys': 1, 'glob': 1, 'tied': 1, 'closedir': 1, 'ioctl': 1, 'socket': 1, 'readlink': 1, 'eval': 1, 'xor': 1, 'readline': 1, 'binmode': 1, 'setservent': 1, 'eof': 1, 'ord': 1, 'bind': 1, 'alarm': 1, 'pipe': 1, 'atan2': 1, 'getgrent': 1, 'exp': 1, 'time': 1, 'push': 1, 'setgrent': 1, 'gt': 1, 'lt': 1, 'or': 1, 'ne': 1, 'm': 1};
    return {
        defaultMode: {
            lexems: [hljs.IDENT_RE],
            contains: PERL_DEFAULT_CONTAINS,
            keywords: PERL_KEYWORDS
        },
        modes: [

            // variables
            {
                className: 'variable',
                begin: '\\$\\d', end: '^'
            },
            {
                className: 'variable',
                begin: '[\\$\\%\\@\\*](\\^\\w\\b|#\\w+(\\:\\:\\w+)*|[^\\s\\w{]|{\\w+}|\\w+(\\:\\:\\w*)*)', end: '^'
            },

            // numbers and strings
            {
                className: 'subst',
                begin: '[$@]\\{', end: '\}',
                lexems: [hljs.IDENT_RE],
                keywords: PERL_KEYWORDS,
                contains: PERL_DEFAULT_CONTAINS,
                relevance: 10
            },
            {
                className: 'number',
                begin: '(\\b0[0-7_]+)|(\\b0x[0-9a-fA-F_]+)|(\\b[1-9][0-9_]*(\\.[0-9_]+)?)|[0_]\\b', end: '^',
                relevance: 0
            },
            {
                className: 'string',
                begin: 'q[qwxr]?\\s*\\(', end: '\\)',
                contains: ['escape', 'subst', 'variable'],
                relevance: 5
            },
            {
                className: 'string',
                begin: 'q[qwxr]?\\s*\\[', end: '\\]',
                contains: ['escape', 'subst', 'variable'],
                relevance: 5
            },
            {
                className: 'string',
                begin: 'q[qwxr]?\\s*\\{', end: '\\}',
                contains: ['escape', 'subst', 'variable'],
                relevance: 5
            },
            {
                className: 'string',
                begin: 'q[qwxr]?\\s*\\|', end: '\\|',
                contains: ['escape', 'subst', 'variable'],
                relevance: 5
            },
            {
                className: 'string',
                begin: 'q[qwxr]?\\s*\\<', end: '\\>',
                contains: ['escape', 'subst', 'variable'],
                relevance: 5
            },
            {
                className: 'string',
                begin: 'qw\\s+q', end: 'q',
                contains: ['escape', 'subst', 'variable'],
                relevance: 5
            },
            {
                className: 'string',
                begin: '\'', end: '\'',
                contains: ['escape'],
                relevance: 0
            },
            {
                className: 'string',
                begin: '"', end: '"',
                contains: ['escape','subst','variable'],
                relevance: 0
            },
            hljs.BACKSLASH_ESCAPE,
            {
                className: 'string',
                begin: '`', end: '`',
                contains: ['escape']
            },

            // regexps
            {
                className: 'regexp',
                begin: '(s|tr|y)/(\\\\.|[^/])*/(\\\\.|[^/])*/[a-z]*', end: '^',
                relevance: 10
            },
            {
                className: 'regexp',
                begin: '(m|qr)?/', end: '/[a-z]*',
                contains: ['escape'],
                relevance: 0 // allows empty "//" which is a common comment delimiter in other languages
            },

            // bareword context
            {
                className: 'string',
                begin: '{\\w+}', end: '^',
                relevance: 0
            },
            {
                className: 'string',
                begin: '\-?\\w+\\s*\\=\\>', end: '^',
                relevance: 0
            },

            // subroutines
            {
                className: 'sub',
                begin: '\\bsub\\b', end: '(\\s*\\(.*?\\))?[;{]',
                lexems: [hljs.IDENT_RE],
                keywords: {'sub':1},
                relevance: 5
            },

            // operators
            {
                className: 'operator',
                begin: '-\\w\\b', end: '^',
                relevance: 0
            },

            // comments
            hljs.HASH_COMMENT_MODE,
            {
                className: 'comment',
                begin: '^(__END__|__DATA__)', end: '\\n$',
                relevance: 5
            },
            // pod
            {
                className: 'pod',
                begin: '\\=\\w', end: '\\=cut'
            }

        ]
    };
}();/*
Language: C++
*/

hljs.LANGUAGES.cpp = function(){
  var CPP_KEYWORDS = {
    'keyword': {'false': 1, 'int': 1, 'float': 1, 'while': 1, 'private': 1, 'char': 1, 'catch': 1, 'export': 1, 'virtual': 1, 'operator': 2, 'sizeof': 2, 'dynamic_cast': 2, 'typedef': 2, 'const_cast': 2, 'const': 1, 'struct': 1, 'for': 1, 'static_cast': 2, 'union': 1, 'namespace': 1, 'unsigned': 1, 'long': 1, 'throw': 1, 'volatile': 2, 'static': 1, 'protected': 1, 'bool': 1, 'template': 1, 'mutable': 1, 'if': 1, 'public': 1, 'friend': 2, 'do': 1, 'return': 1, 'goto': 1, 'auto': 1, 'void': 2, 'enum': 1, 'else': 1, 'break': 1, 'new': 1, 'extern': 1, 'using': 1, 'true': 1, 'class': 1, 'asm': 1, 'case': 1, 'typeid': 1, 'short': 1, 'reinterpret_cast': 2, 'default': 1, 'double': 1, 'register': 1, 'explicit': 1, 'signed': 1, 'typename': 1, 'try': 1, 'this': 1, 'switch': 1, 'continue': 1, 'wchar_t': 1, 'inline': 1, 'delete': 1},
    'built_in': {'std': 1, 'string': 1, 'cin': 1, 'cout': 1, 'cerr': 1, 'clog': 1, 'stringstream': 1, 'istringstream': 1, 'ostringstream': 1, 'auto_ptr': 1, 'deque': 1, 'list': 1, 'queue': 1, 'stack': 1, 'vector': 1, 'map': 1, 'set': 1, 'bitset': 1, 'multiset': 1, 'multimap': 1}
  };
  return {
    defaultMode: {
      lexems: [hljs.UNDERSCORE_IDENT_RE],
      illegal: '</',
      contains: ['comment', 'string', 'number', 'preprocessor', 'stl_container'],
      keywords: CPP_KEYWORDS
    },
    modes: [
      hljs.C_LINE_COMMENT_MODE,
      hljs.C_BLOCK_COMMENT_MODE,
      hljs.C_NUMBER_MODE,
      hljs.QUOTE_STRING_MODE,
      hljs.BACKSLASH_ESCAPE,
      {
        className: 'string',
        begin: '\'', end: '[^\\\\]\'',
        illegal: '[^\\\\][^\']'
      },
      {
        className: 'preprocessor',
        begin: '#', end: '$'
      },
      {
        className: 'stl_container',
        begin: '\\b(deque|list|queue|stack|vector|map|set|bitset|multiset|multimap)\\s*<', end: '>',
        contains: ['stl_container'],
        lexems: [hljs.UNDERSCORE_IDENT_RE],
        keywords: CPP_KEYWORDS,
        relevance: 10
      }
    ]
  };
}();/*
 Language: SQL
 */

hljs.LANGUAGES.sql =
{
    case_insensitive: true,
    defaultMode:
    {
        lexems: [hljs.IDENT_RE],
        contains: ['string', 'number', 'comment'],
        keywords: {
            'keyword': {'all': 1, 'partial': 1, 'global': 1, 'month': 1, 'current_timestamp': 1, 'using': 1, 'go': 1, 'revoke': 1, 'smallint': 1, 'indicator': 1, 'end-exec': 1, 'disconnect': 1, 'zone': 1, 'with': 1, 'character': 1, 'assertion': 1, 'to': 1, 'add': 1, 'current_user': 1, 'usage': 1, 'input': 1, 'local': 1, 'alter': 1, 'match': 1, 'collate': 1, 'real': 1, 'then': 1, 'rollback': 1, 'get': 1, 'read': 1, 'timestamp': 1, 'session_user': 1, 'not': 1, 'integer': 1, 'bit': 1, 'unique': 1, 'day': 1, 'minute': 1, 'desc': 1, 'insert': 1, 'execute': 1, 'like': 1, 'ilike': 2, 'level': 1, 'decimal': 1, 'drop': 1, 'continue': 1, 'isolation': 1, 'found': 1, 'where': 1, 'constraints': 1, 'domain': 1, 'right': 1, 'national': 1, 'some': 1, 'module': 1, 'transaction': 1, 'relative': 1, 'second': 1, 'connect': 1, 'escape': 1, 'close': 1, 'system_user': 1, 'for': 1, 'deferred': 1, 'section': 1, 'cast': 1, 'current': 1, 'sqlstate': 1, 'allocate': 1, 'intersect': 1, 'deallocate': 1, 'numeric': 1, 'public': 1, 'preserve': 1, 'full': 1, 'goto': 1, 'initially': 1, 'asc': 1, 'no': 1, 'key': 1, 'output': 1, 'collation': 1, 'group': 1, 'by': 1, 'union': 1, 'session': 1, 'both': 1, 'last': 1, 'language': 1, 'constraint': 1, 'column': 1, 'of': 1, 'space': 1, 'foreign': 1, 'deferrable': 1, 'prior': 1, 'connection': 1, 'unknown': 1, 'action': 1, 'commit': 1, 'view': 1, 'or': 1, 'first': 1, 'into': 1, 'float': 1, 'year': 1, 'primary': 1, 'cascaded': 1, 'except': 1, 'restrict': 1, 'set': 1, 'references': 1, 'names': 1, 'table': 1, 'outer': 1, 'open': 1, 'select': 1, 'size': 1, 'are': 1, 'rows': 1, 'from': 1, 'prepare': 1, 'distinct': 1, 'leading': 1, 'create': 1, 'only': 1, 'next': 1, 'inner': 1, 'authorization': 1, 'schema': 1, 'corresponding': 1, 'option': 1, 'declare': 1, 'precision': 1, 'immediate': 1, 'else': 1, 'timezone_minute': 1, 'external': 1, 'varying': 1, 'translation': 1, 'true': 1, 'case': 1, 'exception': 1, 'join': 1, 'hour': 1, 'default': 1, 'double': 1, 'scroll': 1, 'value': 1, 'cursor': 1, 'descriptor': 1, 'values': 1, 'dec': 1, 'fetch': 1, 'procedure': 1, 'delete': 1, 'and': 1, 'false': 1, 'int': 1, 'is': 1, 'describe': 1, 'char': 1, 'as': 1, 'at': 1, 'in': 1, 'varchar': 1, 'null': 1, 'trailing': 1, 'any': 1, 'absolute': 1, 'current_time': 1, 'end': 1, 'grant': 1, 'privileges': 1, 'when': 1, 'cross': 1, 'check': 1, 'write': 1, 'current_date': 1, 'pad': 1, 'begin': 1, 'temporary': 1, 'exec': 1, 'time': 1, 'update': 1, 'catalog': 1, 'user': 1, 'sql': 1, 'date': 1, 'on': 1, 'identity': 1, 'timezone_hour': 1, 'natural': 1, 'whenever': 1, 'interval': 1, 'work': 1, 'order': 1, 'cascade': 1, 'diagnostics': 1, 'nchar': 1, 'having': 1, 'left': 1},
            'aggregate': {'count': 1, 'sum': 1, 'min': 1, 'max': 1, 'avg': 1}
        }
    },

    modes: [
        hljs.C_NUMBER_MODE,
        hljs.C_BLOCK_COMMENT_MODE,
        {
            className: 'comment',
            begin: '--', end: '$'
        },
        {
            className: 'string',
            begin: '\'', end: '\'',
            contains: ['escape', 'squote'],
            relevance: 0
        },
        {
            className: 'squote',
            begin: '\'\'', end: '^', noMarkup: true
        },
        {
            className: 'string',
            begin: '"', end: '"',
            contains: [ 'escape', 'dquote'],
            relevance: 0
        },
        {
            className: 'dquote',
            begin: '""', end: '^', noMarkup: true
        },
        {
            className: 'string',
            begin: '`', end: '`',
            contains: ['escape']
        },
        hljs.BACKSLASH_ESCAPE
    ]
};
/*
 Language: Python profile
 Description: Python profiler results
 Author: Brian Beck <exogen@gmail.com>
 */

hljs.LANGUAGES.profile = {
    defaultMode: {
        lexems: [hljs.UNDERSCORE_IDENT_RE],
        contains: ['number', 'builtin', 'filename', 'header', 'summary', 'string', 'function']
    },
    modes: [
        hljs.C_NUMBER_MODE,
        hljs.APOS_STRING_MODE,
        hljs.QUOTE_STRING_MODE,
        {
            className: 'summary',
            begin: 'function calls', end: '$',
            contains: ['number'],
            relevance: 10
        },
        {
            className: 'header',
            begin: '(ncalls|tottime|cumtime)', end: '$',
            lexems: [hljs.IDENT_RE],
            keywords: {'ncalls': 1, 'tottime': 10, 'cumtime': 10, 'filename': 1},
            relevance: 10
        },
        {
            className: 'function',
            begin: '\\(', end: '\\)',
            lexems: [hljs.UNDERSCORE_IDENT_RE],
            contains: ['title']
        },
        {
            className: 'title',
            begin: hljs.UNDERSCORE_IDENT_RE, end: '^'
        },
        {
            className: 'builtin',
            begin: '{', end: '}',
            contains: ['string'],
            excludeBegin: true, excludeEnd: true
        },
        {
            className: 'filename',
            begin: '(/\w|[a-zA-Z_][\da-zA-Z_]+\\.[\da-zA-Z_]{1,3})', end: ':',
            excludeEnd: true
        }
    ]
};
/*
Language: Bash
Author: vah <vahtenberg@gmail.com>
*/

hljs.LANGUAGES.bash = function(){
  var BASH_LITERAL = {'true' : 1, 'false' : 1}
  return {
    defaultMode: {
      lexems: [hljs.IDENT_RE],
      contains: ['string', 'shebang', 'comment', 'number', 'test_condition', 'string', 'variable'],
      keywords: {
        'keyword': {'if' : 1, 'then' : 1, 'else' : 1, 'fi' : 1, 'for' : 1, 'break' : 1, 'continue' : 1, 'while' : 1, 'in' : 1, 'do' : 1, 'done' : 1, 'echo' : 1, 'exit' : 1, 'return' : 1, 'set' : 1, 'declare' : 1},
        'literal': BASH_LITERAL
      }
    },
    case_insensitive: false,
    modes: [
      {
        className: 'shebang',
        begin: '(#!\\/bin\\/bash)|(#!\\/bin\\/sh)',
        end: '^',
        relevance: 10
      },
      hljs.HASH_COMMENT_MODE,
      {
        className: 'test_condition',
        begin: '\\[ ',
        end: ' \\]',
        contains: ['string', 'variable', 'number'],
        lexems: [hljs.IDENT_RE],
        keywords: {
          'literal': BASH_LITERAL
        },
        relevance: 0
      },
      {
        className: 'test_condition',
        begin: '\\[\\[ ',
        end: ' \\]\\]',
        contains: ['string', 'variable', 'number'],
        lexems: [hljs.IDENT_RE],
        keywords: {
          'literal': BASH_LITERAL
        }
      },
      {
        className: 'variable',
        begin: '\\$([a-zA-Z0-9_]+)\\b',
        end: '^'
      },
      {
        className: 'variable',
        begin: '\\$\\{(([^}])|(\\\\}))+\\}',
        end: '^',
        contains: ['number']
      },
      {
        className: 'string',
        begin: '"', end: '"',
        illegal: '\\n',
        contains: ['escape', 'variable'],
        relevance: 0
      },
      {
        className: 'string',
        begin: '"', end: '"',
        illegal: '\\n',
        contains: ['escape', 'variable'],
        relevance: 0
      },
      hljs.BACKSLASH_ESCAPE,
      hljs.C_NUMBER_MODE,
      {
        className: 'comment',
        begin: '\\/\\/', end: '$',
        illegal: '.'
      }
    ]
  };
}();
/*
 Language: HTML, XML
 */

hljs.XML_COMMENT = {
    className: 'comment',
    begin: '<!--', end: '-->'
};
hljs.XML_ATTR = {
    className: 'attribute',
    begin: '\\s[a-zA-Z\\:-]+=', end: '^',
    contains: ['value']
};
hljs.XML_VALUE_QUOT = {
    className: 'value',
    begin: '"', end: '"'
};
hljs.XML_VALUE_APOS = {
    className: 'value',
    begin: '\'', end: '\''
};


hljs.LANGUAGES.xml = {
    defaultMode: {
        contains: ['pi', 'comment', 'cdata', 'tag']
    },
    case_insensitive: true,
    modes: [
        {
            className: 'pi',
            begin: '<\\?', end: '\\?>',
            relevance: 10
        },
        hljs.XML_COMMENT,
        {
            className: 'cdata',
            begin: '<\\!\\[CDATA\\[', end: '\\]\\]>'
        },
        {
            className: 'tag',
            begin: '</?', end: '>',
            contains: ['title', 'tag_internal'],
            relevance: 1.5
        },
        {
            className: 'title',
            begin: '[A-Za-z:_][A-Za-z0-9\\._:-]+', end: '^',
            relevance: 0
        },
        {
            className: 'tag_internal',
            begin: '^', endsWithParent: true, noMarkup: true,
            contains: ['attribute'],
            relevance: 0,
            illegal: '[\\+\\.]'
        },
        hljs.XML_ATTR,
        hljs.XML_VALUE_QUOT,
        hljs.XML_VALUE_APOS
    ]
};

hljs.HTML_TAGS =
{'code': 1, 'kbd': 1, 'font': 1, 'noscript': 1, 'style': 1, 'img': 1, 'title': 1, 'menu': 1, 'tt': 1, 'tr': 1, 'param': 1, 'li': 1, 'tfoot': 1, 'th': 1, 'input': 1, 'td': 1, 'dl': 1, 'blockquote': 1, 'fieldset': 1, 'big': 1, 'dd': 1, 'abbr': 1, 'optgroup': 1, 'dt': 1, 'button': 1, 'isindex': 1, 'p': 1, 'small': 1, 'div': 1, 'dir': 1, 'em': 1, 'frame': 1, 'meta': 1, 'sub': 1, 'bdo': 1, 'label': 1, 'acronym': 1, 'sup': 1, 'body': 1, 'xml': 1, 'basefont': 1, 'base': 1, 'br': 1, 'address': 1, 'strong': 1, 'legend': 1, 'ol': 1, 'script': 1, 'caption': 1, 's': 1, 'col': 1, 'h2': 1, 'h3': 1, 'h1': 1, 'h6': 1, 'h4': 1, 'h5': 1, 'table': 1, 'select': 1, 'noframes': 1, 'span': 1, 'area': 1, 'dfn': 1, 'strike': 1, 'cite': 1, 'thead': 1, 'head': 1, 'option': 1, 'form': 1, 'hr': 1, 'var': 1, 'link': 1, 'b': 1, 'colgroup': 1, 'ul': 1, 'applet': 1, 'del': 1, 'iframe': 1, 'pre': 1, 'frameset': 1, 'ins': 1, 'tbody': 1, 'html': 1, 'samp': 1, 'map': 1, 'object': 1, 'a': 1, 'xmlns': 1, 'center': 1, 'textarea': 1, 'i': 1, 'q': 1, 'u': 1};
hljs.HTML_DOCTYPE = {
    className: 'doctype',
    begin: '<!DOCTYPE', end: '>',
    relevance: 10
};
hljs.HTML_ATTR = {
    className: 'attribute',
    begin: '\\s[a-zA-Z\\:-]+=', end: '^',
    contains: ['value']
};
hljs.HTML_SHORT_ATTR = {
    className: 'attribute',
    begin: ' [a-zA-Z]+', end: '^'
};
hljs.HTML_VALUE = {
    className: 'value',
    begin: '[a-zA-Z0-9]+', end: '^'
};

hljs.LANGUAGES.html = {
    defaultMode: {
        contains: ['tag', 'comment', 'doctype', 'vbscript']
    },
    case_insensitive: true,
    modes: [
        hljs.XML_COMMENT,
        hljs.HTML_DOCTYPE,
        {
            className: 'tag',
            lexems: [hljs.IDENT_RE],
            keywords: hljs.HTML_TAGS,
            begin: '<style', end: '>',
            contains: ['attribute'],
            illegal: '[\\+\\.]',
            starts: 'css'
        },
        {
            className: 'tag',
            lexems: [hljs.IDENT_RE],
            keywords: hljs.HTML_TAGS,
            begin: '<script', end: '>',
            contains: ['attribute'],
            illegal: '[\\+\\.]',
            starts: 'javascript'
        },
        {
            className: 'tag',
            lexems: [hljs.IDENT_RE],
            keywords: hljs.HTML_TAGS,
            begin: '<[A-Za-z/]', end: '>',
            contains: ['attribute'],
            illegal: '[\\+\\.]'
        },
        {
            className: 'css',
            end: '</style>', returnEnd: true,
            subLanguage: 'css'
        },
        {
            className: 'javascript',
            end: '</script>', returnEnd: true,
            subLanguage: 'javascript'
        },
        hljs.HTML_ATTR,
        hljs.HTML_SHORT_ATTR,
        hljs.XML_VALUE_QUOT,
        hljs.XML_VALUE_APOS,
        hljs.HTML_VALUE,
        {
            className: 'vbscript',
            begin: '<%', end: '%>',
            subLanguage: 'vbscript'
        }
    ]
};

/*
 Language: Ini
 */

hljs.LANGUAGES.ini =
{
    case_insensitive: true,
    defaultMode: {
        contains: ['comment', 'title', 'setting'],
        illegal: '[^\\s]'
    },
    modes: [
        {
            className: 'comment',
            begin: ';', end: '$'
        },
        {
            className: 'title',
            begin: '\\[', end: '\\]'
        },
        {
            className: 'setting',
            begin: '^[a-z]+[ \\t]*=[ \\t]*', end: '$',
            contains: ['value']
        },
        {
            className: 'value',
            begin: '^', endsWithParent: true,
            contains: ['string', 'number'],
            lexems: [hljs.IDENT_RE],
            keywords: {'on': 1, 'off': 1, 'true': 1, 'false': 1, 'yes': 1, 'no': 1}
        },
        hljs.QUOTE_STRING_MODE,
        hljs.BACKSLASH_ESCAPE,
        {
            className: 'number',
            begin: '\\d+', end: '^'
        }
    ]
};
/*
Language: Apache
Author: Ruslan Keba <rukeba@gmail.com>
Website: http://rukeba.com/
Description: language definition for Apache configuration files (httpd.conf & .htaccess)
Version 1.1
Date: 2008-12-27
*/

hljs.LANGUAGES.apache =
{
  case_insensitive: true,
  defaultMode: {
    lexems: [hljs.IDENT_RE],
    contains: ['comment', 'sqbracket', 'cbracket', 'number', 'tag', 'string'],
    keywords: {
      'keyword': {
        'acceptfilter': 1,
        'acceptmutex': 1,
        'acceptpathinfo': 1,
        'accessfilename': 1,
        'action': 1,
        'addalt': 1,
        'addaltbyencoding': 1,
        'addaltbytype': 1,
        'addcharset': 1,
        'adddefaultcharset': 1,
        'adddescription': 1,
        'addencoding': 1,
        'addhandler': 1,
        'addicon': 1,
        'addiconbyencoding': 1,
        'addiconbytype': 1,
        'addinputfilter': 1,
        'addlanguage': 1,
        'addmoduleinfo': 1,
        'addoutputfilter': 1,
        'addoutputfilterbytype': 1,
        'addtype': 1,
        'alias': 1,
        'aliasmatch': 1,
        'allow': 1,
        'allowconnect': 1,
        'allowencodedslashes': 1,
        'allowoverride': 1,
        'anonymous': 1,
        'anonymous_logemail': 1,
        'anonymous_mustgiveemail': 1,
        'anonymous_nouserid': 1,
        'anonymous_verifyemail': 1,
        'authbasicauthoritative': 1,
        'authbasicprovider': 1,
        'authdbduserpwquery': 1,
        'authdbduserrealmquery': 1,
        'authdbmgroupfile': 1,
        'authdbmtype': 1,
        'authdbmuserfile': 1,
        'authdefaultauthoritative': 1,
        'authdigestalgorithm': 1,
        'authdigestdomain': 1,
        'authdigestnccheck': 1,
        'authdigestnonceformat': 1,
        'authdigestnoncelifetime': 1,
        'authdigestprovider': 1,
        'authdigestqop': 1,
        'authdigestshmemsize': 1,
        'authgroupfile': 1,
        'authldapbinddn': 1,
        'authldapbindpassword': 1,
        'authldapcharsetconfig': 1,
        'authldapcomparednonserver': 1,
        'authldapdereferencealiases': 1,
        'authldapgroupattribute': 1,
        'authldapgroupattributeisdn': 1,
        'authldapremoteuserattribute': 1,
        'authldapremoteuserisdn': 1,
        'authldapurl': 1,
        'authname': 1,
        'authnprovideralias': 1,
        'authtype': 1,
        'authuserfile': 1,
        'authzdbmauthoritative': 1,
        'authzdbmtype': 1,
        'authzdefaultauthoritative': 1,
        'authzgroupfileauthoritative': 1,
        'authzldapauthoritative': 1,
        'authzownerauthoritative': 1,
        'authzuserauthoritative': 1,
        'balancermember': 1,
        'browsermatch': 1,
        'browsermatchnocase': 1,
        'bufferedlogs': 1,
        'cachedefaultexpire': 1,
        'cachedirlength': 1,
        'cachedirlevels': 1,
        'cachedisable': 1,
        'cacheenable': 1,
        'cachefile': 1,
        'cacheignorecachecontrol': 1,
        'cacheignoreheaders': 1,
        'cacheignorenolastmod': 1,
        'cacheignorequerystring': 1,
        'cachelastmodifiedfactor': 1,
        'cachemaxexpire': 1,
        'cachemaxfilesize': 1,
        'cacheminfilesize': 1,
        'cachenegotiateddocs': 1,
        'cacheroot': 1,
        'cachestorenostore': 1,
        'cachestoreprivate': 1,
        'cgimapextension': 1,
        'charsetdefault': 1,
        'charsetoptions': 1,
        'charsetsourceenc': 1,
        'checkcaseonly': 1,
        'checkspelling': 1,
        'chrootdir': 1,
        'contentdigest': 1,
        'cookiedomain': 1,
        'cookieexpires': 1,
        'cookielog': 1,
        'cookiename': 1,
        'cookiestyle': 1,
        'cookietracking': 1,
        'coredumpdirectory': 1,
        'customlog': 1,
        'dav': 1,
        'davdepthinfinity': 1,
        'davgenericlockdb': 1,
        'davlockdb': 1,
        'davmintimeout': 1,
        'dbdexptime': 1,
        'dbdkeep': 1,
        'dbdmax': 1,
        'dbdmin': 1,
        'dbdparams': 1,
        'dbdpersist': 1,
        'dbdpreparesql': 1,
        'dbdriver': 1,
        'defaulticon': 1,
        'defaultlanguage': 1,
        'defaulttype': 1,
        'deflatebuffersize': 1,
        'deflatecompressionlevel': 1,
        'deflatefilternote': 1,
        'deflatememlevel': 1,
        'deflatewindowsize': 1,
        'deny': 1,
        'directoryindex': 1,
        'directorymatch': 1,
        'directoryslash': 1,
        'documentroot': 1,
        'dumpioinput': 1,
        'dumpiologlevel': 1,
        'dumpiooutput': 1,
        'enableexceptionhook': 1,
        'enablemmap': 1,
        'enablesendfile': 1,
        'errordocument': 1,
        'errorlog': 1,
        'example': 1,
        'expiresactive': 1,
        'expiresbytype': 1,
        'expiresdefault': 1,
        'extendedstatus': 1,
        'extfilterdefine': 1,
        'extfilteroptions': 1,
        'fileetag': 1,
        'filterchain': 1,
        'filterdeclare': 1,
        'filterprotocol': 1,
        'filterprovider': 1,
        'filtertrace': 1,
        'forcelanguagepriority': 1,
        'forcetype': 1,
        'forensiclog': 1,
        'gracefulshutdowntimeout': 1,
        'group': 1,
        'header': 1,
        'headername': 1,
        'hostnamelookups': 1,
        'identitycheck': 1,
        'identitychecktimeout': 1,
        'imapbase': 1,
        'imapdefault': 1,
        'imapmenu': 1,
        'include': 1,
        'indexheadinsert': 1,
        'indexignore': 1,
        'indexoptions': 1,
        'indexorderdefault': 1,
        'indexstylesheet': 1,
        'isapiappendlogtoerrors': 1,
        'isapiappendlogtoquery': 1,
        'isapicachefile': 1,
        'isapifakeasync': 1,
        'isapilognotsupported': 1,
        'isapireadaheadbuffer': 1,
        'keepalive': 1,
        'keepalivetimeout': 1,
        'languagepriority': 1,
        'ldapcacheentries': 1,
        'ldapcachettl': 1,
        'ldapconnectiontimeout': 1,
        'ldapopcacheentries': 1,
        'ldapopcachettl': 1,
        'ldapsharedcachefile': 1,
        'ldapsharedcachesize': 1,
        'ldaptrustedclientcert': 1,
        'ldaptrustedglobalcert': 1,
        'ldaptrustedmode': 1,
        'ldapverifyservercert': 1,
        'limitinternalrecursion': 1,
        'limitrequestbody': 1,
        'limitrequestfields': 1,
        'limitrequestfieldsize': 1,
        'limitrequestline': 1,
        'limitxmlrequestbody': 1,
        'listen': 1,
        'listenbacklog': 1,
        'loadfile': 1,
        'loadmodule': 1,
        'lockfile': 1,
        'logformat': 1,
        'loglevel': 1,
        'maxclients': 1,
        'maxkeepaliverequests': 1,
        'maxmemfree': 1,
        'maxrequestsperchild': 1,
        'maxrequestsperthread': 1,
        'maxspareservers': 1,
        'maxsparethreads': 1,
        'maxthreads': 1,
        'mcachemaxobjectcount': 1,
        'mcachemaxobjectsize': 1,
        'mcachemaxstreamingbuffer': 1,
        'mcacheminobjectsize': 1,
        'mcacheremovalalgorithm': 1,
        'mcachesize': 1,
        'metadir': 1,
        'metafiles': 1,
        'metasuffix': 1,
        'mimemagicfile': 1,
        'minspareservers': 1,
        'minsparethreads': 1,
        'mmapfile': 1,
        'mod_gzip_on': 1,
        'mod_gzip_add_header_count': 1,
        'mod_gzip_keep_workfiles': 1,
        'mod_gzip_dechunk': 1,
        'mod_gzip_min_http': 1,
        'mod_gzip_minimum_file_size': 1,
        'mod_gzip_maximum_file_size': 1,
        'mod_gzip_maximum_inmem_size': 1,
        'mod_gzip_temp_dir': 1,
        'mod_gzip_item_include': 1,
        'mod_gzip_item_exclude': 1,
        'mod_gzip_command_version': 1,
        'mod_gzip_can_negotiate': 1,
        'mod_gzip_handle_methods': 1,
        'mod_gzip_static_suffix': 1,
        'mod_gzip_send_vary': 1,
        'mod_gzip_update_static': 1,
        'modmimeusepathinfo': 1,
        'multiviewsmatch': 1,
        'namevirtualhost': 1,
        'noproxy': 1,
        'nwssltrustedcerts': 1,
        'nwsslupgradeable': 1,
        'options': 1,
        'order': 1,
        'passenv': 1,
        'pidfile': 1,
        'protocolecho': 1,
        'proxybadheader': 1,
        'proxyblock': 1,
        'proxydomain': 1,
        'proxyerroroverride': 1,
        'proxyftpdircharset': 1,
        'proxyiobuffersize': 1,
        'proxymaxforwards': 1,
        'proxypass': 1,
        'proxypassinterpolateenv': 1,
        'proxypassmatch': 1,
        'proxypassreverse': 1,
        'proxypassreversecookiedomain': 1,
        'proxypassreversecookiepath': 1,
        'proxypreservehost': 1,
        'proxyreceivebuffersize': 1,
        'proxyremote': 1,
        'proxyremotematch': 1,
        'proxyrequests': 1,
        'proxyset': 1,
        'proxystatus': 1,
        'proxytimeout': 1,
        'proxyvia': 1,
        'readmename': 1,
        'receivebuffersize': 1,
        'redirect': 1,
        'redirectmatch': 1,
        'redirectpermanent': 1,
        'redirecttemp': 1,
        'removecharset': 1,
        'removeencoding': 1,
        'removehandler': 1,
        'removeinputfilter': 1,
        'removelanguage': 1,
        'removeoutputfilter': 1,
        'removetype': 1,
        'requestheader': 1,
        'require': 2,
        'rewritebase': 1,
        'rewritecond': 10,
        'rewriteengine': 1,
        'rewritelock': 1,
        'rewritelog': 1,
        'rewriteloglevel': 1,
        'rewritemap': 1,
        'rewriteoptions': 1,
        'rewriterule': 10,
        'rlimitcpu': 1,
        'rlimitmem': 1,
        'rlimitnproc': 1,
        'satisfy': 1,
        'scoreboardfile': 1,
        'script': 1,
        'scriptalias': 1,
        'scriptaliasmatch': 1,
        'scriptinterpretersource': 1,
        'scriptlog': 1,
        'scriptlogbuffer': 1,
        'scriptloglength': 1,
        'scriptsock': 1,
        'securelisten': 1,
        'seerequesttail': 1,
        'sendbuffersize': 1,
        'serveradmin': 1,
        'serveralias': 1,
        'serverlimit': 1,
        'servername': 1,
        'serverpath': 1,
        'serverroot': 1,
        'serversignature': 1,
        'servertokens': 1,
        'setenv': 1,
        'setenvif': 1,
        'setenvifnocase': 1,
        'sethandler': 1,
        'setinputfilter': 1,
        'setoutputfilter': 1,
        'ssienableaccess': 1,
        'ssiendtag': 1,
        'ssierrormsg': 1,
        'ssistarttag': 1,
        'ssitimeformat': 1,
        'ssiundefinedecho': 1,
        'sslcacertificatefile': 1,
        'sslcacertificatepath': 1,
        'sslcadnrequestfile': 1,
        'sslcadnrequestpath': 1,
        'sslcarevocationfile': 1,
        'sslcarevocationpath': 1,
        'sslcertificatechainfile': 1,
        'sslcertificatefile': 1,
        'sslcertificatekeyfile': 1,
        'sslciphersuite': 1,
        'sslcryptodevice': 1,
        'sslengine': 1,
        'sslhonorciperorder': 1,
        'sslmutex': 1,
        'ssloptions': 1,
        'sslpassphrasedialog': 1,
        'sslprotocol': 1,
        'sslproxycacertificatefile': 1,
        'sslproxycacertificatepath': 1,
        'sslproxycarevocationfile': 1,
        'sslproxycarevocationpath': 1,
        'sslproxyciphersuite': 1,
        'sslproxyengine': 1,
        'sslproxymachinecertificatefile': 1,
        'sslproxymachinecertificatepath': 1,
        'sslproxyprotocol': 1,
        'sslproxyverify': 1,
        'sslproxyverifydepth': 1,
        'sslrandomseed': 1,
        'sslrequire': 1,
        'sslrequiressl': 1,
        'sslsessioncache': 1,
        'sslsessioncachetimeout': 1,
        'sslusername': 1,
        'sslverifyclient': 1,
        'sslverifydepth': 1,
        'startservers': 1,
        'startthreads': 1,
        'substitute': 1,
        'suexecusergroup': 1,
        'threadlimit': 1,
        'threadsperchild': 1,
        'threadstacksize': 1,
        'timeout': 1,
        'traceenable': 1,
        'transferlog': 1,
        'typesconfig': 1,
        'unsetenv': 1,
        'usecanonicalname': 1,
        'usecanonicalphysicalport': 1,
        'user': 1,
        'userdir': 1,
        'virtualdocumentroot': 1,
        'virtualdocumentrootip': 1,
        'virtualscriptalias': 1,
        'virtualscriptaliasip': 1,
        'win32disableacceptex': 1,
        'xbithack': 1
      },
      'literal': {'on': 1, 'off': 1}
    }
  },
  modes: [
    hljs.HASH_COMMENT_MODE,
    {
      /* TODO: check tag content: Location, Files, VirtualHost, ... */
      className: 'tag',
      begin: '</?', end: '>'
    },
    {
      className: 'sqbracket',
      begin: '\\s\\[', end: '\\]$'
    },
    {
      className: 'cbracket',
      begin: '[\\$%]\\{', end: '\\}',
      contains: ['cbracket', 'number']
    },
    {
      className: 'number',
      begin: '[\\$%]\\d+', end: '^'
    },
    hljs.QUOTE_STRING_MODE,
    hljs.BACKSLASH_ESCAPE
  ]
};
/*
 Language: Ruby
 Author: Anton Kovalyov <anton@kovalyov.net>
 Contributors: Peter Leonov <gojpeg@yandex.ru>
 */

hljs.LANGUAGES.ruby = function() {
    var RUBY_IDENT_RE = '[a-zA-Z_][a-zA-Z0-9_]*(\\!|\\?)?';
    var RUBY_DEFAULT_CONTAINS = ['comment', 'string', 'char', 'class', 'function', 'symbol', 'number', 'variable', 'regexp_container']
    var RUBY_KEYWORDS = {
        'keyword': {'and': 1, 'false': 1, 'then': 1, 'defined': 1, 'module': 1, 'in': 1, 'return': 1, 'redo': 1, 'if': 1, 'BEGIN': 1, 'retry': 1, 'end': 1, 'for': 1, 'true': 1, 'self': 1, 'when': 1, 'next': 1, 'until': 1, 'do': 1, 'begin': 1, 'unless': 1, 'END': 1, 'rescue': 1, 'nil': 1, 'else': 1, 'break': 1, 'undef': 1, 'not': 1, 'super': 1, 'class': 1, 'case': 1, 'require': 1, 'yield': 1, 'alias': 1, 'while': 1, 'ensure': 1, 'elsif': 1, 'or': 1, 'def': 1},
        'keymethods': {'__id__': 1, '__send__': 1, 'abort': 1, 'abs': 1, 'all?': 1, 'allocate': 1, 'ancestors': 1, 'any?': 1, 'arity': 1, 'assoc': 1, 'at': 1, 'at_exit': 1, 'autoload': 1, 'autoload?': 1, 'between?': 1, 'binding': 1, 'binmode': 1, 'block_given?': 1, 'call': 1, 'callcc': 1, 'caller': 1, 'capitalize': 1, 'capitalize!': 1, 'casecmp': 1, 'catch': 1, 'ceil': 1, 'center': 1, 'chomp': 1, 'chomp!': 1, 'chop': 1, 'chop!': 1, 'chr': 1, 'class': 1, 'class_eval': 1, 'class_variable_defined?': 1, 'class_variables': 1, 'clear': 1, 'clone': 1, 'close': 1, 'close_read': 1, 'close_write': 1, 'closed?': 1, 'coerce': 1, 'collect': 1, 'collect!': 1, 'compact': 1, 'compact!': 1, 'concat': 1, 'const_defined?': 1, 'const_get': 1, 'const_missing': 1, 'const_set': 1, 'constants': 1, 'count': 1, 'crypt': 1, 'default': 1, 'default_proc': 1, 'delete': 1, 'delete!': 1, 'delete_at': 1, 'delete_if': 1, 'detect': 1, 'display': 1, 'div': 1, 'divmod': 1, 'downcase': 1, 'downcase!': 1, 'downto': 1, 'dump': 1, 'dup': 1, 'each': 1, 'each_byte': 1, 'each_index': 1, 'each_key': 1, 'each_line': 1, 'each_pair': 1, 'each_value': 1, 'each_with_index': 1, 'empty?': 1, 'entries': 1, 'eof': 1, 'eof?': 1, 'eql?': 1, 'equal?': 1, 'eval': 1, 'exec': 1, 'exit': 1, 'exit!': 1, 'extend': 1, 'fail': 1, 'fcntl': 1, 'fetch': 1, 'fileno': 1, 'fill': 1, 'find': 1, 'find_all': 1, 'first': 1, 'flatten': 1, 'flatten!': 1, 'floor': 1, 'flush': 1, 'for_fd': 1, 'foreach': 1, 'fork': 1, 'format': 1, 'freeze': 1, 'frozen?': 1, 'fsync': 1, 'getc': 1, 'gets': 1, 'global_variables': 1, 'grep': 1, 'gsub': 1, 'gsub!': 1, 'has_key?': 1, 'has_value?': 1, 'hash': 1, 'hex': 1, 'id': 1, 'include?': 1, 'included_modules': 1, 'index': 1, 'indexes': 1, 'indices': 1, 'induced_from': 1, 'inject': 1, 'insert': 1, 'inspect': 1, 'instance_eval': 1, 'instance_method': 1, 'instance_methods': 1, 'instance_of?': 1, 'instance_variable_defined?': 1, 'instance_variable_get': 1, 'instance_variable_set': 1, 'instance_variables': 1, 'integer?': 1, 'intern': 1, 'invert': 1, 'ioctl': 1, 'is_a?': 1, 'isatty': 1, 'iterator?': 1, 'join': 1, 'key?': 1, 'keys': 1, 'kind_of?': 1, 'lambda': 1, 'last': 1, 'length': 1, 'lineno': 1, 'ljust': 1, 'load': 1, 'local_variables': 1, 'loop': 1, 'lstrip': 1, 'lstrip!': 1, 'map': 1, 'map!': 1, 'match': 1, 'max': 1, 'member?': 1, 'merge': 1, 'merge!': 1, 'method': 1, 'method_defined?': 1, 'method_missing': 1, 'methods': 1, 'min': 1, 'module_eval': 1, 'modulo': 1, 'name': 1, 'nesting': 1, 'new': 1, 'next': 1, 'next!': 1, 'nil?': 1, 'nitems': 1, 'nonzero?': 1, 'object_id': 1, 'oct': 1, 'open': 1, 'pack': 1, 'partition': 1, 'pid': 1, 'pipe': 1, 'pop': 1, 'popen': 1, 'pos': 1, 'prec': 1, 'prec_f': 1, 'prec_i': 1, 'print': 1, 'printf': 1, 'private_class_method': 1, 'private_instance_methods': 1, 'private_method_defined?': 1, 'private_methods': 1, 'proc': 1, 'protected_instance_methods': 1, 'protected_method_defined?': 1, 'protected_methods': 1, 'public_class_method': 1, 'public_instance_methods': 1, 'public_method_defined?': 1, 'public_methods': 1, 'push': 1, 'putc': 1, 'puts': 1, 'quo': 1, 'raise': 1, 'rand': 1, 'rassoc': 1, 'read': 1, 'read_nonblock': 1, 'readchar': 1, 'readline': 1, 'readlines': 1, 'readpartial': 1, 'rehash': 1, 'reject': 1, 'reject!': 1, 'remainder': 1, 'reopen': 1, 'replace': 1, 'require': 1, 'respond_to?': 1, 'reverse': 1, 'reverse!': 1, 'reverse_each': 1, 'rewind': 1, 'rindex': 1, 'rjust': 1, 'round': 1, 'rstrip': 1, 'rstrip!': 1, 'scan': 1, 'seek': 1, 'select': 1, 'send': 1, 'set_trace_func': 1, 'shift': 1, 'singleton_method_added': 1, 'singleton_methods': 1, 'size': 1, 'sleep': 1, 'slice': 1, 'slice!': 1, 'sort': 1, 'sort!': 1, 'sort_by': 1, 'split': 1, 'sprintf': 1, 'squeeze': 1, 'squeeze!': 1, 'srand': 1, 'stat': 1, 'step': 1, 'store': 1, 'strip': 1, 'strip!': 1, 'sub': 1, 'sub!': 1, 'succ': 1, 'succ!': 1, 'sum': 1, 'superclass': 1, 'swapcase': 1, 'swapcase!': 1, 'sync': 1, 'syscall': 1, 'sysopen': 1, 'sysread': 1, 'sysseek': 1, 'system': 1, 'syswrite': 1, 'taint': 1, 'tainted?': 1, 'tell': 1, 'test': 1, 'throw': 1, 'times': 1, 'to_a': 1, 'to_ary': 1, 'to_f': 1, 'to_hash': 1, 'to_i': 1, 'to_int': 1, 'to_io': 1, 'to_proc': 1, 'to_s': 1, 'to_str': 1, 'to_sym': 1, 'tr': 1, 'tr!': 1, 'tr_s': 1, 'tr_s!': 1, 'trace_var': 1, 'transpose': 1, 'trap': 1, 'truncate': 1, 'tty?': 1, 'type': 1, 'ungetc': 1, 'uniq': 1, 'uniq!': 1, 'unpack': 1, 'unshift': 1, 'untaint': 1, 'untrace_var': 1, 'upcase': 1, 'upcase!': 1, 'update': 1, 'upto': 1, 'value?': 1, 'values': 1, 'values_at': 1, 'warn': 1, 'write': 1, 'write_nonblock': 1, 'zero?': 1, 'zip': 1}
    }
    return {
        defaultMode: {
            lexems: [RUBY_IDENT_RE],
            contains: RUBY_DEFAULT_CONTAINS,
            keywords: RUBY_KEYWORDS
        },
        modes: [
            hljs.HASH_COMMENT_MODE,
            {
                className: 'comment',
                begin: '^\\=begin', end: '^\\=end',
                relevance: 10
            },
            {
                className: 'comment',
                begin: '^__END__', end: '\\n$'
            },
            {
                className: 'params',
                begin: '\\(', end: '\\)',
                lexems: [RUBY_IDENT_RE],
                keywords: RUBY_KEYWORDS,
                contains: RUBY_DEFAULT_CONTAINS
            },
            {
                className: 'function',
                begin: '\\bdef\\b', end: '$|;',
                lexems: [RUBY_IDENT_RE],
                keywords: RUBY_KEYWORDS,
                contains: ['title', 'params', 'comment']
            },
            {
                className: 'class',
                begin: '\\b(class|module)\\b', end: '$',
                lexems: [hljs.UNDERSCORE_IDENT_RE],
                keywords: RUBY_KEYWORDS,
                contains: ['title', 'inheritance', 'comment'],
                keywords: {'class': 1, 'module': 1}
            },
            {
                className: 'title',
                begin: '[A-Za-z_]\\w*(::\\w+)*(\\?|\\!)?', end: '^',
                relevance: 0
            },
            {
                className: 'inheritance',
                begin: '<\\s*', end: '^',
                contains: ['parent']
            },
            {
                className: 'parent',
                begin: '(' + hljs.IDENT_RE + '::)?' + hljs.IDENT_RE, end: '^'
            },
            {
                className: 'number',
                begin: '(\\b0[0-7_]+)|(\\b0x[0-9a-fA-F_]+)|(\\b[1-9][0-9_]*(\\.[0-9_]+)?)|[0_]\\b', end: '^',
                relevance: 0
            },
            {
                className: 'number',
                begin: '\\?\\w', end: '^'
            },
            {
                className: 'string',
                begin: '\'', end: '\'',
                contains: ['escape', 'subst'],
                relevance: 0
            },
            {
                className: 'string',
                begin: '"', end: '"',
                contains: ['escape', 'subst'],
                relevance: 0
            },
            {
                className: 'string',
                begin: '%[qw]?\\(', end: '\\)',
                contains: ['escape', 'subst'],
                relevance: 10
            },
            {
                className: 'string',
                begin: '%[qw]?\\[', end: '\\]',
                contains: ['escape', 'subst'],
                relevance: 10
            },
            {
                className: 'string',
                begin: '%[qw]?{', end: '}',
                contains: ['escape', 'subst'],
                relevance: 10
            },
            {
                className: 'string',
                begin: '%[qw]?<', end: '>',
                contains: ['escape', 'subst'],
                relevance: 10
            },
            {
                className: 'string',
                begin: '%[qw]?/', end: '/',
                contains: ['escape', 'subst'],
                relevance: 10
            },
            {
                className: 'string',
                begin: '%[qw]?%', end: '%',
                contains: ['escape', 'subst'],
                relevance: 10
            },
            {
                className: 'string',
                begin: '%[qw]?-', end: '-',
                contains: ['escape', 'subst'],
                relevance: 10
            },
            {
                className: 'string',
                begin: '%[qw]?\\|', end: '\\|',
                contains: ['escape', 'subst'],
                relevance: 10
            },
            {
                className: 'symbol',
                begin: ':' + RUBY_IDENT_RE, end: '^'
            },
            hljs.BACKSLASH_ESCAPE,
            {
                className: 'subst',
                begin: '#\\{', end: '}',
                lexems: [RUBY_IDENT_RE],
                keywords: RUBY_KEYWORDS,
                contains: RUBY_DEFAULT_CONTAINS
            },
            {
                className: 'regexp_container',
                begin: '(' + hljs.RE_STARTERS_RE + ')\\s*', end: '^', noMarkup: true,
                contains: ['comment', 'regexp'],
                relevance: 0
            },
            {
                className: 'regexp',
                begin: '/', end: '/[a-z]*',
                illegal: '\\n',
                contains: ['escape']
            },
            {
                className: 'variable',
                begin: '(\\$\\W)|((\\$|\\@\\@?)(\\w+))', end: '^'
            }
        ]
    };
}();
/*
Language: CSS
Requires:  html-xml.js
*/

hljs.LANGUAGES.css = {
  defaultMode: {
    contains: ['id', 'class', 'attr_selector', 'rules', 'comment'],
    keywords: hljs.HTML_TAGS,
    lexems: [hljs.IDENT_RE],
    illegal: '='
  },
  case_insensitive: true,
  modes: [
    {
      className: 'id',
      begin: '\\#[A-Za-z0-9_-]+', end: '^'
    },
    {
      className: 'class',
      begin: '\\.[A-Za-z0-9_-]+', end: '^',
      relevance: 0
    },
    {
      className: 'attr_selector',
      begin: '\\[', end: '\\]',
      illegal: '$'
    },
    {
      className: 'rules',
      begin: '{', end: '}',
      contains: ['rule', 'comment'],
      illegal: '[^\\s]'
    },
    {
      className: 'rule',
      begin: '[A-Z\\_\\.\\-]+\\s*:', end: ';', endsWithParent: true,
      lexems: ['[A-Za-z-]+'],
      keywords: {'play-during': 1, 'counter-reset': 1, 'counter-increment': 1, 'min-height': 1, 'quotes': 1, 'border-top': 1, 'pitch': 1, 'font': 1, 'pause': 1, 'list-style-image': 1, 'border-width': 1, 'cue': 1, 'outline-width': 1, 'border-left': 1, 'elevation': 1, 'richness': 1, 'speech-rate': 1, 'border-bottom': 1, 'border-spacing': 1, 'background': 1, 'list-style-type': 1, 'text-align': 1, 'page-break-inside': 1, 'orphans': 1, 'page-break-before': 1, 'text-transform': 1, 'line-height': 1, 'padding-left': 1, 'font-size': 1, 'right': 1, 'word-spacing': 1, 'padding-top': 1, 'outline-style': 1, 'bottom': 1, 'content': 1, 'border-right-style': 1, 'padding-right': 1, 'border-left-style': 1, 'voice-family': 1, 'background-color': 1, 'border-bottom-color': 1, 'outline-color': 1, 'unicode-bidi': 1, 'max-width': 1, 'font-family': 1, 'caption-side': 1, 'border-right-width': 1, 'pause-before': 1, 'border-top-style': 1, 'color': 1, 'border-collapse': 1, 'border-bottom-width': 1, 'float': 1, 'height': 1, 'max-height': 1, 'margin-right': 1, 'border-top-width': 1, 'speak': 1, 'speak-header': 1, 'top': 1, 'cue-before': 1, 'min-width': 1, 'width': 1, 'font-variant': 1, 'border-top-color': 1, 'background-position': 1, 'empty-cells': 1, 'direction': 1, 'border-right': 1, 'visibility': 1, 'padding': 1, 'border-style': 1, 'background-attachment': 1, 'overflow': 1, 'border-bottom-style': 1, 'cursor': 1, 'margin': 1, 'display': 1, 'border-left-width': 1, 'letter-spacing': 1, 'vertical-align': 1, 'clip': 1, 'border-color': 1, 'list-style': 1, 'padding-bottom': 1, 'pause-after': 1, 'speak-numeral': 1, 'margin-left': 1, 'widows': 1, 'border': 1, 'font-style': 1, 'border-left-color': 1, 'pitch-range': 1, 'background-repeat': 1, 'table-layout': 1, 'margin-bottom': 1, 'speak-punctuation': 1, 'font-weight': 1, 'border-right-color': 1, 'page-break-after': 1, 'position': 1, 'white-space': 1, 'text-indent': 1, 'background-image': 1, 'volume': 1, 'stress': 1, 'outline': 1, 'clear': 1, 'z-index': 1, 'text-decoration': 1, 'margin-top': 1, 'azimuth': 1, 'cue-after': 1, 'left': 1, 'list-style-position': 1},
      contains: ['value']
    },
    hljs.C_BLOCK_COMMENT_MODE,
    {
      className: 'value',
      begin: '^', endsWithParent: true, excludeEnd: true,
      contains: ['function', 'number', 'hexcolor', 'string']
    },
    {
      className: 'number',
      begin: hljs.NUMBER_RE, end: '^'
    },
    {
      className: 'hexcolor',
      begin: '\\#[0-9A-F]+', end: '^'
    },
    {
      className: 'function',
      begin: hljs.IDENT_RE + '\\(', end: '\\)',
      contains: ['params']
    },
    {
      className: 'params',
      begin: '^', endsWithParent: true, excludeEnd: true,
      contains: ['number', 'string']
    },
    hljs.APOS_STRING_MODE,
    hljs.QUOTE_STRING_MODE
  ]
};
/*
 Language: Java
 Author: Vsevolod Solovyov <vsevolod.solovyov@gmail.com>
 */

hljs.LANGUAGES.java = {
    defaultMode: {
        lexems: [hljs.UNDERSCORE_IDENT_RE],
        contains: ['javadoc', 'comment', 'string', 'class', 'number', 'annotation'],
        keywords: {'false': 1, 'synchronized': 1, 'int': 1, 'abstract': 1, 'float': 1, 'private': 1, 'char': 1, 'interface': 1, 'boolean': 1, 'static': 1, 'null': 1, 'if': 1, 'const': 1, 'for': 1, 'true': 1, 'while': 1, 'long': 1, 'throw': 1, 'strictfp': 1, 'finally': 1, 'protected': 1, 'extends': 1, 'import': 1, 'native': 1, 'final': 1, 'implements': 1, 'return': 1, 'void': 1, 'enum': 1, 'else': 1, 'break': 1, 'transient': 1, 'new': 1, 'catch': 1, 'instanceof': 1, 'byte': 1, 'super': 1, 'class': 1, 'volatile': 1, 'case': 1, 'assert': 1, 'short': 1, 'package': 1, 'default': 1, 'double': 1, 'public': 1, 'try': 1, 'this': 1, 'switch': 1, 'continue': 1, 'throws': 1}
    },
    modes: [
        {
            className: 'class',
            lexems: [hljs.UNDERSCORE_IDENT_RE],
            begin: '(class |interface )', end: '{',
            illegal: ':',
            keywords: {'class': 1, 'interface': 1},
            contains: ['inheritance', 'title']
        },
        {
            className: 'inheritance',
            begin: '(implements|extends)', end: '^', noMarkup: true,
            lexems: [hljs.IDENT_RE],
            keywords: {'extends': 1, 'implements': 1},
            relevance: 10
        },
        {
            className: 'title',
            begin: hljs.UNDERSCORE_IDENT_RE, end: '^'
        },
        {
            className: 'params',
            begin: '\\(', end: '\\)',
            contains: ['string', 'annotation']
        },
        hljs.C_NUMBER_MODE,
        hljs.APOS_STRING_MODE,
        hljs.QUOTE_STRING_MODE,
        hljs.BACKSLASH_ESCAPE,
        hljs.C_LINE_COMMENT_MODE,
        {
            className: 'javadoc',
            begin: '/\\*\\*', end: '\\*/',
            contains: ['javadoctag'],
            relevance: 10
        },
        {
            className: 'javadoctag',
            begin: '@[A-Za-z]+', end: '^'
        },
        hljs.C_BLOCK_COMMENT_MODE,
        {
            className: 'annotation',
            begin: '@[A-Za-z]+', end: '^'
        }
    ]
};
/*
 Language: Javascript
 */

hljs.LANGUAGES.javascript = {
    defaultMode: {
        lexems: [hljs.UNDERSCORE_IDENT_RE],
        contains: ['string', 'comment', 'number', 'regexp_container', 'function'],
        keywords: {
            'keyword': {'in': 1, 'if': 1, 'for': 1, 'while': 1, 'finally': 1, 'var': 1, 'new': 1, 'function': 1, 'do': 1, 'return': 1, 'void': 1, 'else': 1, 'break': 1, 'catch': 1, 'instanceof': 1, 'with': 1, 'throw': 1, 'case': 1, 'default': 1, 'try': 1, 'this': 1, 'switch': 1, 'continue': 1, 'typeof': 1, 'delete': 1},
            'literal': {'true': 1, 'false': 1, 'null': 1}
        }
    },
    modes: [
        hljs.C_LINE_COMMENT_MODE,
        hljs.C_BLOCK_COMMENT_MODE,
        hljs.C_NUMBER_MODE,
        hljs.APOS_STRING_MODE,
        hljs.QUOTE_STRING_MODE,
        hljs.BACKSLASH_ESCAPE,
        {
            className: 'regexp_container',
            begin: '(' + hljs.RE_STARTERS_RE + '|case|return|throw)\\s*', end: '^', noMarkup: true,
            lexems: [hljs.IDENT_RE],
            keywords: {'return': 1, 'throw': 1, 'case': 1},
            contains: ['comment', 'regexp'],
            relevance: 0
        },
        {
            className: 'regexp',
            begin: '/.*?[^\\\\/]/[gim]*', end: '^'
        },
        {
            className: 'function',
            begin: '\\bfunction\\b', end: '{',
            lexems: [hljs.UNDERSCORE_IDENT_RE],
            keywords: {'function': 1},
            contains: ['title', 'params']
        },
        {
            className: 'title',
            begin: '[A-Za-z$_][0-9A-Za-z$_]*', end: '^'
        },
        {
            className: 'params',
            begin: '\\(', end: '\\)',
            contains: ['string', 'comment']
        }
    ]
};
/*
 Language: PHP
 Author: Victor Karamzin <Victor.Karamzin@enterra-inc.com>
 */

hljs.LANGUAGES.php = {
    defaultMode: {
        lexems: [hljs.IDENT_RE],
        contains: ['comment', 'number', 'string', 'variable', 'preprocessor'],
        keywords: {'and': 1, 'include_once': 1, 'list': 1, 'abstract': 1, 'global': 1, 'private': 1, 'echo': 1, 'interface': 1, 'as': 1, 'static': 1, 'endswitch': 1, 'array': 1, 'null': 1, 'if': 1, 'endwhile': 1, 'or': 1, 'const': 1, 'for': 1, 'endforeach': 1, 'self': 1, 'var': 1, 'while': 1, 'isset': 1, 'public': 1, 'protected': 1, 'exit': 1, 'foreach': 1, 'throw': 1, 'elseif': 1, 'extends': 1, 'include': 1, '__FILE__': 1, 'empty': 1, 'require_once': 1, 'function': 1, 'do': 1, 'xor': 1, 'return': 1, 'implements': 1, 'parent': 1, 'clone': 1, 'use': 1, '__CLASS__': 1, '__LINE__': 1, 'else': 1, 'break': 1, 'print': 1, 'eval': 1, 'new': 1, 'catch': 1, '__METHOD__': 1, 'class': 1, 'case': 1, 'exception': 1, 'php_user_filter': 1, 'default': 1, 'die': 1, 'require': 1, '__FUNCTION__': 1, 'enddeclare': 1, 'final': 1, 'try': 1, 'this': 1, 'switch': 1, 'continue': 1, 'endfor': 1, 'endif': 1, 'declare': 1, 'unset': 1}
    },
    case_insensitive: true,
    modes: [
        hljs.C_LINE_COMMENT_MODE,
        hljs.HASH_COMMENT_MODE,
        {
            className: 'comment',
            begin: '/\\*', end: '\\*/',
            contains: ['phpdoc']
        },
        {
            className: 'phpdoc',
            begin: '\\s@[A-Za-z]+', end: '^',
            relevance: 10
        },
        hljs.C_NUMBER_MODE,
        {
            className: 'string',
            begin: '\'', end: '\'',
            contains: ['escape'],
            relevance: 0
        },
        {
            className: 'string',
            begin: '"', end: '"',
            contains: ['escape'],
            relevance: 0
        },
        hljs.BACKSLASH_ESCAPE,
        {
            className: 'variable',
            begin: '\\$[a-zA-Z_\x7f-\xff][a-zA-Z0-9_\x7f-\xff]*', end: '^'
        },
        {
            className: 'preprocessor',
            begin: '<\\?php', end: '^',
            relevance: 10
        },
        {
            className: 'preprocessor',
            begin: '\\?>', end: '^'
        }
    ]
};
/*
Language: diff
Description: Unified and context diff
Author: Vasily Polovnyov <vast@whiteants.net>
*/

hljs.LANGUAGES.diff = {
  case_insensitive: true,
  defaultMode: {
    contains: ['chunk', 'header', 'addition', 'deletion', 'change']
  },
  modes: [
    {
      className: 'chunk',
      begin: '^\\@\\@ +\\-\\d+,\\d+ +\\+\\d+,\\d+ +\\@\\@$', end:'^',
      relevance: 10
    },
    {
      className: 'chunk',
      begin: '^\\*\\*\\* +\\d+,\\d+ +\\*\\*\\*\\*$', end: '^',
      relevance: 10
    },
    {
      className: 'chunk',
      begin: '^\\-\\-\\- +\\d+,\\d+ +\\-\\-\\-\\-$', end: '^',
      relevance: 10
    },
    {
      className: 'header',
      begin: 'Index: ', end: '$'
    },
    {
      className: 'header',
      begin: '=====', end: '=====$'
    },
    {
      className: 'header',
      begin: '^\\-\\-\\-', end: '$'
    },
    {
      className: 'header',
      begin: '^\\*{3} ', end: '$'
    },
    {
      className: 'header',
      begin: '^\\+\\+\\+', end: '$'
    },
    {
      className: 'header',
      begin: '\\*{5}', end: '\\*{5}$'
    },
    {
      className: 'addition',
      begin: '^\\+', end: '$'
    },
    {
      className: 'deletion',
      begin: '^\\-', end: '$'
    },
    {
      className: 'change',
      begin: '^\\!', end: '$'
    }
  ]
}
