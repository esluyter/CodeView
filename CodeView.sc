CodeView : SCViewHolder {
  var <parent, <font, italicfont, <>matchChars, <>debug = false, <tabWidth = 2;
  var <palette, <colorScheme, <tokens;
  var <customTokens, <customColors;
  var <>modKeyHandler, <>keyUpAction;

  *new { |parent, bounds|
    ^super.new.init(parent, bounds);
  }

  init { |argparent, argbounds|
    parent = argparent;

    matchChars = [
      [$", $"],
      [$', $'],
      [$[, $]],
      [$(, $)],
      [${, $}]
    ];

    tokens = (
      keyword: "var|arg|this|true|false|currentEnvironment|topEnvironment|thisProcess|thisThread|thisFunction",
      envvar: "\\~\\w+",
      class: "[A-Z]\\w*",
      method: "\\.[a-z_]\\w*|\\W[a-z_]\\w+\\s*(\\(|\\{)",
      number: "(\\-)?((\\d+(\\.\\d+)?)|pi|inf)",
      symbol: "((')((\\\\{2})*|(.*?[^\\\\](\\\\{2})*))\\2)|(\\\\\\w+)",
      key: "(\\w+):",
      string: "([" ++ ('"'.asString) ++ "])((\\\\{2})*|(.*?[^\\\\](\\\\{2})*))\\1",
      comment: "//.*?(\\n|$)",
      longComment: "/\\*.*?\\*/",
      partialComment: "/\\*.*?(\\n|$)",
      punctuation: "[<>\\&\\{\\}\\(\\)\\[\\]\\.\\,\\;:!\\=\\+\\-\\*\\/\\%\\|]"
    );

    this.view = TextView(parent, argbounds)
    .enterInterpretsSelection_(false)
    .mouseUpAction_({
      this.changed(\mouseClicked);
    })
    .keyDownAction_({ |...args|
      this.handleKey(*args);
    })
    .keyUpAction_({ |view, char, mod, unicode, keycode, key|
      // broadcast escape
      if (char.ascii == 27) {
        this.changed(\escapePressed);
      } {
        this.changed(\keyPressed);
      };
      keyUpAction.(view, char, mod, unicode, keycode, key);
    });

    this.font_(Font.monospace);

    this.oneDarkColorScheme;
  }

  open { |path|
    view.open(path);
    this.colorize;
  }

  makeCompleteWindow { |apoint| // in offset from top left corner
    var bounds;
    apoint = apoint ?? (600@0);
    bounds = Rect(apoint.x, Window.screenBounds.height - apoint.y, 300, 300);
    ^CodeViewCompleteWindow(this, bounds);
  }

  colorScheme_ { |value|
    colorScheme = value;

    palette = QPalette.auto(colorScheme[\background], colorScheme[\background]);
		palette.base = colorScheme[\background];
    palette.baseText = colorScheme[\text];
    palette.highlight = colorScheme[\selectionBackground];
    palette.highlightText = colorScheme[\selectionText];

    view.palette = palette;
    this.colorize;
  }

  oneLightColorScheme {
    this.colorScheme_((
      background: Color.new255(250, 250, 250),
      text: Color.new255(56, 58, 66),
      selectionBackground: Color.new255(229, 229, 230),
      selectionText: Color.new255(56, 58, 66),
      keyword: Color.new255(168, 28, 166),
      envvar: Color.new255(230, 85, 68),
      class: Color.new255(194, 133, 0),
      method: Color.new255(60, 116, 246),
      number: Color.new255(156, 109, 0),
      symbol: Color.new255(0, 131, 190),
      key: Color.new255(0, 131, 190).darken(Color.gray(0.5)),
      string: Color.new255(77, 162, 75),
      comment: Color.new255(160, 161, 167),
      punctuation: Color.new255(100, 120, 140)
    ));
  }

  lightColorScheme {
    this.colorScheme_((
      background: Color.new255(255, 252, 240),
      text: Color.new255(9, 3, 0),
      selectionBackground: Color.new255(60, 90, 163),
      selectionText: Color.new255(95, 164, 215),
      keyword: Color.new255(141, 53, 0),
      envvar: Color.new255(153, 51, 62),
      class: Color.new255(52, 0, 207),
      method: Color.new255(9, 3, 0),
      number: Color.new255(211, 59, 118),
      symbol: Color.new255(101, 162, 0),
      key: Color.new255(101, 162, 0).darken(Color.gray(0.5)),
      string: Color.new255(140, 166, 108),
      comment: Color.new255(215, 174, 155),
      punctuation: Color.new255(120, 100, 80)
    ));
  }

  oneDarkColorScheme {
    this.colorScheme_((
      background: Color.new255(44, 50, 60),
      text: Color.new255(171, 178, 192),
      selectionBackground: Color.new255(62, 68, 82),
      selectionText: Color.new255(171, 178, 192),
      keyword: Color.new255(199, 117, 223),
      envvar: Color.new255(226, 107, 115),
      class: Color.new255(230, 193, 118),
      method: Color.new255(93, 174, 242),
      number: Color.new255(210, 155, 98),
      symbol: Color.new255(81, 182, 195),
      key: Color.new255(114, 210, 145).darken(Color.gray(0.5)),
      string: Color.new255(145, 188, 114),
      comment: Color.new255(92, 99, 113),
      punctuation: Color(0.6, 0.7, 0.8)
    ));
  }

  darkColorScheme {
    this.colorScheme_((
      background: Color(0.18, 0.21, 0.25),
      text: Color.gray(0.85),
      selectionBackground: Color(0.188, 0.286, 0.5),
      selectionText: Color.gray(0.9),
      keyword: Color(0.2, 0.4, 0.7),
      envvar: Color(0.9, 0.9, 0.6),
      class: Color(0.6, 0.95, 0.95),
      method: Color.gray(0.85),
      number: Color(1, 0.8, 0.9),
      symbol: Color(0.5, 0.9, 0.6),
      key: Color.new255(114, 210, 145).darken(Color.gray(0.6)),
      string: Color(0.6, 1, 0.7),
      comment: Color(0.7, 0.4, 0.5),
      punctuation: Color(0.6, 0.7, 0.8)
    ));
  }

  customTokens_ { |value|
    customTokens = value;
    this.colorize;
  }

  customColors_ { |value|
    customColors = value;
    this.colorize;
  }

  string {
    ^view.string;
  }
  string_ { |string|
    view.string_(string);
    this.colorize;
  }

  font_ { |afont|
    font = afont;
    italicfont = font.copy.italic_(true);
    this.colorize;
  }

  select { |start, size|
    view.select(start, size);
  }
  selectionStart {
    ^view.selectionStart;
  }
  selectionSize {
    ^view.selectionSize;
  }

  /* -------- PRIVATE ----------- */
  colorize { |wholething = true|
    var start, end, proposedStart, proposedEnd;

    if (view.string.size == 0) {
      ^false; // can't colorize nothing
    };

    // set prelim start & end
    if (wholething) {
      proposedStart = 0; proposedEnd = view.string.size;
    } {
      ([0] ++ view.string.findAll($\n) ++ [view.string.size]).do { |linebreak|
        if (linebreak < view.selectionStart) {
          proposedStart = linebreak;
        } {
          if (proposedEnd.isNil) { proposedEnd = linebreak };
        };
      };
    };

    start = proposedStart ?? 0;
    end = proposedEnd ?? view.string.size;

    // extend boundary for long tokens
    [\longComment, \string, \symbol].do { |thing|
      var regexp = tokens[thing];

      view.string.findRegexp(regexp, 0).do({ |item|
        var itemStart = item[0], itemEnd = itemStart + item[1].size;

        //[start, end, "item:", itemStart, itemEnd].postcs;

        if ((itemStart <= proposedEnd) && (itemEnd >= proposedStart)) {
          if (itemStart < start) { start = itemStart };
          if (itemEnd > end) { end = itemEnd };
        };
      });
    };



    // reset everything
    view.setStringColor(colorScheme[\text], start, end - start);
    view.setFont(font, start, end - start);

    // methods & punctuation
    [\method, \punctuation].do { |thing|
      var color = colorScheme[thing];
      var regexp = tokens[thing];

      view.string.findRegexp(regexp, 0).select({ |item|
        (item[0] >= start) && (item[0] < end);
      }).do { |result|
        view.setStringColor(color, result[0], result[1].size);
      };
    };

    // non-wordchar delimited things
    [\number, \envvar, \keyword].do { |thing|
      var color = colorScheme[thing];
      var regexp = "(\\W|^)(" ++ tokens[thing] ++ ")(\\W|$)";

      view.string.findRegexp(regexp, 0).select({ |item|
        (item[0] >= start) && (item[0] < end) && (("^(" ++ tokens[thing] ++ ")$").matchRegexp(item[1]));
      }).do { |result|
        view.setStringColor(color, result[0], result[1].size);
      };
    };

    // classes
    [\class].do { |thing|
      var color = colorScheme[thing];
      var regexp = "(\\W|^)(" ++ tokens[thing] ++ ")(\\W|$)";

      view.string.findRegexp(regexp, 0).select({ |item|
        (item[0] >= start) && (item[0] < end) && (("^(" ++ tokens[thing] ++ ")$").matchRegexp(item[1]));
      }).select({ |item|
        Class.allClasses.detect({ |class| class.name == item[1].asSymbol }).notNil;
      }).do { |result|
        view.setStringColor(color, result[0], result[1].size);
      };
    };

    // other language items
    [\key, \symbol, \string].do { |thing|
      var color = colorScheme[thing];
      var regexp = tokens[thing];

      view.string.findRegexp(regexp, 0).select({ |item|
        (item[0] >= start) && (item[0] < end);
      }).do { |result|
        view.setStringColor(color, result[0], result[1].size);
      };
    };

    // comments
    [\comment, \partialComment, \longComment].do { |thing|
      var color = colorScheme[\comment];
      var regexp = tokens[thing];

      view.string.findRegexp(regexp, 0).select({ |item|
        (item[0] >= start) && (item[0] < end);
      }).do { |result|
        view.setStringColor(color, result[0], result[1].size);
        view.setFont(italicfont, result[0], result[1].size);
      };
    };

    // custom
    if (customTokens.notNil) {
      customTokens.keysValuesDo { |thing, regexp|
        var color = customColors[thing];

        if (color.notNil) {
          view.string.findRegexp(regexp, 0).select({ |item|
            (item[0] >= start) && (item[0] < end);
          }).do { |result|
            view.setStringColor(color, result[0], result[1].size);
          };
        };
      };
    };
  }

  indentAt { |lineStart|
    view.setString(String.newFrom($ .dup(tabWidth)), lineStart, 0);
    ^tabWidth;
  }

  deindentAt { |lineStart|
    var currentIndentSpaces = view.string.findRegexpAt("(\\s*)", lineStart)[1];
    var currentTabOffset = currentIndentSpaces % tabWidth; // the amount off the tab grid the line is

    if (currentIndentSpaces > 0) {
      if (currentTabOffset == 0) {
        view.setString("", lineStart, tabWidth);
        ^(0 - tabWidth);
      } {
        view.setString("", lineStart, currentTabOffset);
        ^(0 - currentTabOffset);
      };
    } {
      ^0;
    };
  }

  braceBalance { |lineToCursor, nextChar|
    var balance = 0;
    var extraNewLine = false;

    matchChars.do { |arr|
      var beginChar = arr[0], endChar = arr[1];
      balance = balance + lineToCursor.findAll(beginChar).size - lineToCursor.findAll(endChar).size;
    };

    balance = max(balance, -1);
    balance = min(balance, 1);

    if (balance == 1) {
      if ((nextChar == $}) || (nextChar == $)) || (nextChar == $])) {
        extraNewLine = true;
      };
    };

    ^[balance * tabWidth, extraNewLine];
  }

  getOuterParens {
    var str = view.string;
    var selectionStart = view.selectionStart;
    var prePtr = selectionStart, postPtr = selectionStart - 1;
    var start, end = postPtr, parenBalance = 0;

    while { (prePtr > 0) && (postPtr < str.size)} {
      while { (prePtr > 0) && (parenBalance >= 0) } {
        prePtr = prePtr - 1;
        if (str[prePtr] == $() { parenBalance = parenBalance - 1 };
        if (str[prePtr] == $)) { parenBalance = parenBalance + 1 };
      };
      while { (postPtr < (str.size - 1)) && (parenBalance != 0) } {
        postPtr = postPtr + 1;
        if (str[postPtr] == $() { parenBalance = parenBalance - 1 };
        if (str[postPtr] == $)) { parenBalance = parenBalance + 1 };
      };
      if ((parenBalance == 0) && (prePtr != start) && (postPtr != end)) {
        start = prePtr;
        end = postPtr;
      };
    };

    if (start.notNil) {
      view.select(start, end - start + 1);
      ^str[start..end];
    } {
      view.select(0, str.size);
      ^str;
    };
  }

  getTokenAtCursor { |getPrevToken = false, cursorOverride|
    var str = view.string;
    var selectionStart = cursorOverride ?? max(view.selectionStart - 1, 0);
    var token, type, start;
    var prevToken = [];

    if ("\\s".matchRegexp(str[selectionStart].asString)) { selectionStart = selectionStart + 1 };

    [\punctuation, \method].do { |thing|
      var regexp = tokens[thing];

      var item = str.findRegexp(regexp, 0).select({ |item|
        var itemStart = item[0], itemEnd = item[0] + item[1].size;
        (selectionStart >= itemStart) && (selectionStart < itemEnd);
      })[0];

      if (item.notNil) {
        start = item[0];
        token = item[1];
        type = thing;
      };
    };

    // non-wordchar delimited things
    [\number, \class, \envvar, \keyword].do { |thing|
      var regexp = "(\\W|^)(" ++ tokens[thing] ++ ")(\\W|$)";

      var item = str.findRegexp(regexp, 0).select({ |item|
        var itemStart = item[0], itemEnd = item[0] + item[1].size;
        (selectionStart >= itemStart) && (selectionStart < itemEnd) && (("^(" ++ tokens[thing] ++ ")$").matchRegexp(item[1]));
      })[0];

      if (item.notNil) {
        start = item[0];
        token = item[1];
        type = thing;
      };
    };

    // everything else
    [\key, \symbol, \string, \comment, \longComment].do { |thing|
      var regexp = tokens[thing];

      var item = str.findRegexp(regexp, 0).select({ |item|
        var itemStart = item[0], itemEnd = item[0] + item[1].size;
        (selectionStart >= itemStart) && (selectionStart < itemEnd);
      })[0];

      if (item.notNil) {
        start = item[0];
        token = item[1];
        type = thing;
      };
    };

    // last resort - just take word
    if (token.isNil) {
      var regexp = "\\w+";
      var item = str.findRegexp(regexp, 0).select({ |item|
        var itemStart = item[0], itemEnd = item[0] + item[1].size;
        (selectionStart >= itemStart) && (selectionStart < itemEnd);
      })[0];

      if (item.notNil) {
        start = item[0];
        token = item[1];
        type = \word;
      };
    };

    // very last resort -- just take char
    if (token.isNil) {
      start = selectionStart;
      token = str[selectionStart];
      type = \char;
    };

    // get previous token if necessary
    if (getPrevToken) {
      prevToken = this.getTokenAtCursor(false, start - 1);
    };

    // take . out of method name
    if (type == \method) {
      start = start + 1;
      token = token[1..];
    };

    ^([token.asString.stripWhiteSpace, type] ++ prevToken);
  }

  interpret { |toInterpret|
    try {
      if (toInterpret.stripWhiteSpace != "") {
        ("-> " ++ toInterpret.interpret).postln;
      };
    } { |error|
      error.reportError;
    };
  }

  handleKey { |view, char, mod, unicode, keycode, key|
    var selectionStart = view.selectionStart;
    var selectionSize = view.selectionSize;

    var stringFromSelectionStart = view.string[selectionStart..];
    var stringForward = view.string[(selectionStart + selectionSize)..];
    var selectedString = view.selectedString;
    var stringBackward = view.string[..(selectionStart - 1)].reverse;
    var cursorOnFirstLine = stringBackward.find($\n).isNil;
    var cursorOnLastLine = stringForward.find($\n).isNil;

    var lineStart = if (cursorOnFirstLine) { 0 } {
      selectionStart - stringBackward.find($\n);
    };
    var lineEnd = if (cursorOnLastLine) {
      selectionStart + selectionSize + stringForward.size;
    } {
      selectionStart + selectionSize + stringForward.find($\n);
    };
    var lineSize = lineEnd - lineStart;
    var lineToCursor = view.string[lineStart..(selectionStart - 1)];
    var currentLineEnd = (selectionStart + (stringFromSelectionStart.find($\n) ?? 0));
    var currentLine = (if ((currentLineEnd - lineStart) == 0) { "" } { view.string[lineStart..(currentLineEnd - 1)] });

    var currentIndentSpaces = currentLine.findRegexpAt("(\\s*)", 0)[1];
    var currentIndent = currentIndentSpaces / tabWidth;
    var lineCodeStart = lineStart + currentIndentSpaces;

    var toDelete, nextChar, runningOffset = 0, extraNewLine, currentToken;

    if (debug) { ["view", view, "char", char, "mod", mod, "unicode", unicode, "keycode", keycode, "key", key].postcs; };

    // Add matching character if necessary
    matchChars.do { |arr|
      var beginChar = arr[0], endChar = arr[1];
      if (char == beginChar) {
        if (selectionSize == 0) {
          view.setString(beginChar ++ endChar, selectionStart, 0);
          view.select(selectionStart + 1, 0);
        } {
          view.selectedString = beginChar ++ view.selectedString ++ endChar;
          view.select(selectionStart + 1, selectionSize);
        };
        this.colorize(false);
        ^true;
      };
    };

    // Delete matching character or tab, if necessary
    if (key == 16777219) { // backspace
      if (selectionSize == 0) {
        toDelete = view.string[selectionStart - 1];
        nextChar = view.string[selectionStart];

        if ((selectionStart == lineCodeStart) && (currentIndent > 0)) {
          this.deindentAt(lineStart);
          ^true;
        };

        matchChars.do { |arr|
          var beginChar = arr[0], endChar = arr[1];
          if ((toDelete == beginChar) && (nextChar == endChar)) {
            view.setString("", selectionStart - 1, 2);
            ^true;
          };
        };

        view.setString("", selectionStart - 1, 1);
        this.colorize(false);
        ^true;
      };
    };

    // Tab indents, shift-tab deindents line/selection
    if ((key == 16777217) || (key == 16777218)) { // tab and shift-tab, respectively
      if (view.selectionSize == 0) {
        if (mod.isShift) {
          this.deindentAt(lineStart);
        } {
          this.indentAt(lineStart);
        }
      } {
        ([0] ++ view.string.findAll($\n).collect(_ + 1)) // all lines..
        .select(_ >= lineStart).select(_ < lineEnd) //.. between selection
        .do { |lineStart, i|
          lineStart = lineStart + runningOffset; // account for previous indents

          runningOffset = runningOffset + if (mod.isShift) {
            this.deindentAt(lineStart);
          } {
            this.indentAt(lineStart);
          };
        };
      };
      ^true;
    };

    // Up on top line goes to beginning
    if ((key == 16777235) && cursorOnFirstLine) { // up
      if (mod.isCmd.not && mod.isAlt.not) {
        view.select(0, if (mod.isShift) { selectionStart + selectionSize } { 0 });
        ^true;
      };
    };

    // Down on bottom line goes to end
    if ((key == 16777237) && cursorOnLastLine && mod.isShift.not) { // down
      if (mod.isCmd.not && mod.isAlt.not) {
        view.select(view.string.size, 0);
        ^true;
      };
    };

    // Execute code if necessary, or insert newline with correct indent
    if (key == 16777220) { // enter
      if (mod.isCmd || mod.isShift) { // shift-enter & cmd-enter...

        if (selectionSize != 0) {
          this.interpret(view.selectedString);
        } {
          if (mod.isShift) {
            this.interpret(currentLine);
            view.select(lineStart, lineSize);
          } {
            this.interpret(this.getOuterParens);
          };

          { view.select(selectionStart, selectionSize) }.defer(0.1);
        };

      } { // ...otherwise insert newline with correct indent
        # runningOffset, extraNewLine = this.braceBalance(lineToCursor, stringForward[0]); // add or subtract from total indent
        if (extraNewLine) {
          view.setString("\n" ++ String.newFrom($ .dup(currentIndentSpaces)), selectionStart, selectionSize);
          view.select(selectionStart, 0);
        };
        view.setString("\n" ++ String.newFrom($ .dup(max(currentIndentSpaces + runningOffset, 0))), selectionStart, selectionSize);
      };

      ^true;
    };

    // cmd-D look up documentation
    if ((key == 68) && (mod.isCmd)) {
      if (selectionSize != 0) {
        HelpBrowser.openHelpFor(view.selectedString.stripWhiteSpace);
        ^true;
      } {
        currentToken = this.getTokenAtCursor(true);

        if ((currentToken[1] == \method) && (currentToken[3] == \class)) {
          HelpBrowser.goTo(SCDoc.findHelpFile(currentToken[2]) ++ "#*" ++ currentToken[0]);
          ^true;
        };

        if ([\class, \keyword, \method, \word].indexOf(currentToken[1]).notNil) {
          HelpBrowser.openHelpFor(currentToken[0]);
          ^true;
        };

        if (currentToken[1] == \envvar) {
          HelpBrowser.openHelpFor("Environment");
          ^true;
        };

        if (currentToken[1] == \punctuation) {
          HelpBrowser.goTo(SCDoc.helpTargetUrl +/+ "Overviews/SymbolicNotations.html")
          ^true;
        };

        if (currentToken[1] == \key) {
          HelpBrowser.goTo(SCDoc.helpTargetUrl +/+
            "Reference/Syntax-Shortcuts.html")
          ^true;
        };

        if ([\number, \symbol, \string].indexOf(currentToken[1]).notNil) {
          HelpBrowser.goTo(SCDoc.helpTargetUrl +/+ "Reference/Literals.html#" ++ currentToken[1].asString.capitalize ++ "s");
          ^true;
        };

        if ([\comment, \longComment].indexOf(currentToken[1]).notNil) {
          HelpBrowser.goTo(SCDoc.helpTargetUrl +/+ "Reference/Comments.html");
          ^true;
        };
      };
    };

    // don't handle modifiers or escape...
    if ((char.ascii < 32)) {
      ^modKeyHandler.(view, char, mod, unicode, keycode, key);
    };

    // ...otherwise insert character
    view.setString(char.asString, selectionStart, selectionSize);
    this.colorize(false);
    ^true;
  }
}
