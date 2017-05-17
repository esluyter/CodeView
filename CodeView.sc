CodeView : SCViewHolder {
  var <parent, <font, italicfont, <>matchChars, <>debug = false, <tabWidth = 2;
  var <palette, <colorScheme, <tokens;
  var <customTokens, <customColors;
  var <>modKeyHandler, <>keyUpAction;
  var paste, suppressKeyPress = false, undoHistory, redoHistory;
  var colorizeRout;
  var <>interpretArgs;

  *new { |parent, bounds|
    ^super.new.init(parent, bounds);
  }

  init { |argparent, argbounds|
    parent = argparent;

    this.resetHistory;

    matchChars = [
      [$", $"],
      [$', $'],
      [$[, $]],
      [$(, $)],
      [${, $}]
    ];

    tokens = (
      keyword: "var|arg|this|true|false|currentEnvironment|topEnvironment|thisProcess|thisThread|thisFunction|thisCueList",
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

    this.view = CodeViewNew(parent, argbounds)
    .enterInterpretsSelection_(false)
    .mouseUpAction_({
      this.changed(\mouseClicked);
    })
    .keyDownAction_({ |...args|
      this.handleKey(*args);
    })
    .keyUpAction_({ |view, char, mod, unicode, keycode, key|
      var pasteSize;
      // broadcast escape
      if (char.ascii == 27) {
        this.changed(\escapePressed);
      } {
        if (suppressKeyPress.not) { this.changed(\keyPressed) };
      };
      if (paste.notNil) {
        pasteSize = view.string.size - paste[\stringSize];
        this.colorize(false, paste[\start], paste[\start] + pasteSize);
        this.changed(\textInserted, paste[\start], pasteSize);
        paste = nil;
      };
      keyUpAction.(view, char, mod, unicode, keycode, key);
    });

    this.font_(Font.monospace);

    this.oneLightColorScheme;
  }

  open { |path|
    view.open(path);
    this.colorize;
  }

  makeCompleteWindow { |bounds, parentWindow| // in offset from top left corner
    bounds = bounds ?? Rect(600, 0, 300, 300);
    bounds = bounds.left_(bounds.left + parentWindow.bounds.left).top_(parentWindow.bounds.top + parentWindow.bounds.height - bounds.top);
    ^CodeViewCompleteWindow(this, bounds, parentWindow);
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
    this.changed(\colorScheme);
  }

  oneLightColorScheme {
    this.colorScheme_((
      background: Color.new255(250, 250, 250),
      text: Color.new255(56, 58, 66),
      selectionBackground: Color.new255(225, 225, 228),
      selectionText: Color.new255(126, 126, 126),
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

  paperLightColorScheme {
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

  brightDarkColorScheme {
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
    view.string_(string.reject({|char| (char.ascii < 32 && (char != $\n)) || (char.ascii == 127)}));
    this.colorize;
  }

  font_ { |afont|
    font = afont;
    italicfont = font.copy.italic_(true);
    this.view.font_(afont);
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

  resetHistory {
    undoHistory = [];
    redoHistory = [];
  }
  undo {
    var string, start, size, oldstring, item, isWordChar = true, isDelete, prevstart, prevstring;
    item = undoHistory.pop;
    if (item.isNil) { ^false };

    #string, start, size, oldstring = item;
    prevstart = start;
    isDelete = string.size == 0;

    // undo to previous non-wordchar
    while {isWordChar || isDelete} {
      redoHistory = redoHistory.add([string, start, size, oldstring]); // add this to redo history
      this.setString(oldstring, start, string.size, false); // don't add this to the history
      view.select(start + size, 0);

      prevstart = start;
      prevstring = string;

      item = undoHistory.pop;
      if (item.isNil) { ^false };

      #string, start, size, oldstring = item;
      isWordChar = (string.size == 1 && "\\w".matchRegexp(string)
        && ((prevstart - start).abs < 2) && "\\w".matchRegexp(prevstring));
      isDelete = isDelete && (string.size == 0);
    };

    if ((prevstart - start).abs < 2 && (string.size == 1 && "\\s".matchRegexp(string))) { // undo immediately preceding space
      redoHistory = redoHistory.add([string, start, size, oldstring]); // add this to redo history
      this.setString(oldstring, start, string.size, false); // don't add this to the history
      view.select(start, 0);
    } { // otherwise, don't undo
      undoHistory = undoHistory.add([string, start, size, oldstring]); // add unused back to undo history
    };

    ^true;
  }
  redo {
    var string, start, size, oldstring, item, isWordChar = true, isDelete, prevstart, prevstring;

    item = redoHistory.pop;
    if (item.isNil) { ^false };

    #string, start, size, oldstring = item;
    prevstart = start;
    isDelete = string.size == 0;

    // undo to previous non-wordchar
    while {isWordChar || isDelete} {
      undoHistory = undoHistory.add([string, start, size, oldstring]); // add this back to undo history
      this.setString(string, start, size, false); // don't add this to the history
      view.select(start + string.size, 0);

      prevstart = start;
      prevstring = string;

      item = redoHistory.pop;
      if (item.isNil) { ^false };

      #string, start, size, oldstring = item;
      isWordChar = (string.size == 1 && "\\w".matchRegexp(string)
        && ((prevstart - start).abs < 2) && "\\w|\\s".matchRegexp(prevstring));
      isDelete = isDelete && (string.size == 0);
    };

    redoHistory = redoHistory.add([string, start, size, oldstring]); // add unused back to redo history

    ^true;
  }

  editable {
    ^view.editable;
  }
  editable_ { |bool|
    view.editable_(bool);
  }

  /* -------- PRIVATE ----------- */
  colorize { |wholething = true, proposedStart, proposedEnd, flexibleBounds = true|
    /*
    var start, end, foundEnd = false;
    var lastToken = "", lastTokenStart = 0;

    if (view.string.size == 0) {
      ^false; // can't colorize nothing
    };

    if (flexibleBounds.not) {
      start = proposedStart;
      end = proposedEnd;
    } {
      // set prelim start & end
      if (wholething) {
        proposedStart = 0; proposedEnd = 0; //proposedEnd = view.string.size;

        colorizeRout.stop;
        if (view.string.size > 1000) {
          colorizeRout = {
            0.01.wait;
            while {proposedEnd < view.string.size} {
              proposedEnd = min(view.string.size, proposedStart + 1000);
              //[proposedStart, proposedEnd].postln;
              this.colorize(false, proposedStart, proposedEnd, false);
              proposedStart = proposedEnd;
              0.3.wait;
            };
          }.fork(AppClock);
        } {
          this.colorize(false, 0, view.string.size);
        };

        ^true;
      } {
        proposedStart = proposedStart ?? view.selectionStart;
        proposedEnd = proposedEnd ?? (view.selectionStart + view.selectionSize);

        start = proposedStart;

        // expand to line break
        ([0] ++ view.string.findAll($\n) ++ [view.string.size]).do { |linebreak|
          if (linebreak < start) {
            proposedStart = linebreak;
          } {
            if (foundEnd.not && (linebreak > proposedEnd)) {
              proposedEnd = linebreak;
              foundEnd = true;
            };
          };
        };
      };

      start = proposedStart;
      end = proposedEnd;

      // extend boundary for long tokens
      [\longComment, \string, \symbol].do { |thing|
        var regexp = tokens[thing];

        view.string.findRegexp(regexp, 0).do({ |item|
          var itemStart = item[0], itemEnd = itemStart + item[1].size;

          if ((itemStart <= proposedEnd) && (itemEnd >= proposedStart)) {
            if (itemStart < start) { proposedStart = max(start - 500, itemStart) };
            if (itemEnd > end) { proposedEnd = min(end + 500, itemEnd) };
          };
        });
      };

      start = proposedStart;
      end = proposedEnd;
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

        lastTokenStart = result[0];
        lastToken = result[1];
      };
    };

    // non-wordchar delimited things
    [\number, \envvar, \keyword].do { |thing|
      var color = colorScheme[thing];
      var regexp = "(\\W|^)(" ++ tokens[thing] ++ ")(\\W|$)";

      view.string.findRegexp(regexp, 0).select({ |item|
        (item[0] >= start) && (item[0] < end) && (("^(" ++ tokens[thing] ++ ")$").matchRegexp(item[1]));
      }).do { |result|
        if (abs(lastTokenStart - result[0]) < min(lastToken.size, result[1].size) // if the last token is close to this one
            && lastToken.contains(result[1])) { // and contains this one,
          // skip it
        } {
          view.setStringColor(color, result[0], result[1].size);

          lastTokenStart = result[0];
          lastToken = result[1];
        };
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
        if (abs(lastTokenStart - result[0]) < min(lastToken.size, result[1].size) // if the last token is close to this one
            && lastToken.contains(result[1])) { // and contains this one,
          // skip it
        } {
          view.setStringColor(color, result[0], result[1].size);

          lastTokenStart = result[0];
          lastToken = result[1];
        };
      };
    };

    // other language items
    [\key, \symbol, \string].do { |thing|
      var color = colorScheme[thing];
      var regexp = tokens[thing];

      view.string.findRegexp(regexp, 0).select({ |item|
        ((item[0] >= start) && (item[0] < end))
          || ((item[0] + item[1].size >= start) && (item[0] + item[1].size < end))
          || ((item[0] < start) && (item[0] + item[1].size > end));
      }).do { |result|
        var thisSize;

        if (abs(lastTokenStart - result[0]) < min(lastToken.size, result[1].size) // if the last token is close to this one
            && lastToken.contains(result[1])) { // and contains this one,
          // skip it
        } {
          thisSize = min(end, result[0] + result[1].size) - result[0]; // only do the amount in this range
          view.setStringColor(color, result[0], thisSize);

          lastTokenStart = result[0];
          lastToken = result[1];
        };
      };
    };

    // comments
    [\comment, \partialComment, \longComment].do { |thing|
      var color = colorScheme[\comment];
      var regexp = tokens[thing];

      view.string.findRegexp(regexp, 0).select({ |item|
        ((item[0] >= start) && (item[0] < end))
          || ((item[0] + item[1].size >= start) && (item[0] + item[1].size < end))
          || ((item[0] < start) && (item[0] + item[1].size > end));
      }).do { |result|
        var thisSize;

        if (abs(lastTokenStart - result[0]) < min(lastToken.size, result[1].size) // if the last token is close to this one
            && lastToken.contains(result[1])) { // and contains this one,
          // skip it
        } {
          thisSize = min(end, result[0] + result[1].size) - result[0]; // only do the amount in this range
          view.setStringColor(color, result[0], thisSize);
          view.setFont(italicfont, result[0], thisSize);

          lastTokenStart = result[0];
          lastToken = result[1];
        };
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
    */
  }

  indentAt { |lineStart|
    this.setString(String.newFrom($ .dup(tabWidth)), lineStart, 0);
    ^tabWidth;
  }

  deindentAt { |lineStart|
    var currentIndentSpaces = view.string.findRegexpAt("(\\h*)", lineStart)[1];
    var currentTabOffset = currentIndentSpaces % tabWidth; // the amount off the tab grid the line is

    if (currentIndentSpaces > 0) {
      if (currentTabOffset == 0) {
        this.setString("", lineStart, tabWidth);
        ^(0 - tabWidth);
      } {
        this.setString("", lineStart, currentTabOffset);
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

  getTokenAtCursor { |getPrevToken = false, cursorOverride, getStart = false|
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

        // fix method bug returning e.g. "if ("
        if (type == \method && (token.last == $_ || token.last.isAlphaNum).not) {
          // strip beginning whitespace
          while {token[0].isAlphaNum.not} {
            start = start + 1;
            token = token[1..];
          };

          if (selectionStart == (start + token.lastIndex)) { // is cursor on punctuation?
            if (getPrevToken) {
              prevToken = [token, type];
              if (getStart) { prevToken = prevToken.add(start) };
              prevToken[0] = prevToken[0].findRegexpAt("\\w+")[0];
            };

            start = start + token.lastIndex;
            token = token.last.asString;
            type = \punctuation;

            if (getStart) {
              ^([token, type, start] ++ prevToken);
            } {
              ^([token, type] ++ prevToken);
            };
          } {
            token = "." ++ token.findRegexpAt("\\w+")[0];
            start = start - 1;
          };
        };
      };
    };

    // non-wordchar delimited things
    [\number, \class, \envvar, \keyword].do { |thing|
      var regexp = "(\\W|^)(" ++ tokens[thing] ++ ")(\\W|$)";

      var item = str.findRegexp(regexp, 0).select({ |item|
        var itemStart = item[0], itemEnd = item[0] + item[1].size;

        var itemString = item[1], strippedString = item[1].stripWhiteSpace;

        (selectionStart >= itemStart) && (selectionStart < itemEnd)
        && (("^(" ++ tokens[thing] ++ ")$").matchRegexp(item[1]))
        && (itemString == strippedString);
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
      prevToken = this.getTokenAtCursor(false, start - 1, getStart);
    };

    // take . out of method name
    if (type == \method) {
      start = start + 1;
      token = token[1..];
    };

    ^([token.asString, type] ++ if (getStart) { start } { [] } ++ prevToken);
  }

  setString { |aString, start, size, addToHistory = true|
    if (addToHistory) { this.addToHistory(aString, start, size) };
    view.setString(aString, start, size);
    this.colorize(false, start, start + aString.size);
    this.changed(\textInserted, start, aString.size);
  }

  interpret { |toInterpret|
    var argNames = "", argValues = [], compiledFunc;

    if (toInterpret.stripWhiteSpace == "") { ^false };

    if (interpretArgs.notNil) {
      argNames = [];
      interpretArgs.keysValuesDo { |k, v|
        argNames = argNames.add(k);
        argValues = argValues.add(v);
      };
      argNames = "|" ++ argNames.join(", ") ++ "|";
    };

    toInterpret = "{ " ++ argNames ++ "\n" ++ toInterpret ++ "\n}";

    try {
      compiledFunc = toInterpret.interpret;
      if (compiledFunc.isNil) {
        PostView.postln("ERROR: Compile error. See IDE post window for details.");
        ^false;
      };

      ("-> " ++ compiledFunc.valueArray(argValues)).postln;
      ^true;
    } { |error|
      error.reportError;
      ^false;
    };
  }

  addToHistory { |string, start, size|
    var oldstring = "";
    if (size > 0) {
      oldstring = view.string[start..(start + size - 1)];
    };
    undoHistory = undoHistory.add([string, start, size, oldstring]);
    redoHistory = [];
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
    var currentLineEnd = (selectionStart + (stringFromSelectionStart.find($\n) ?? stringFromSelectionStart.size));
    var currentLine = (if ((currentLineEnd - lineStart) == 0) { "" } { view.string[lineStart..(currentLineEnd - 1)] });

    var currentIndentSpaces = currentLine.findRegexpAt("(\\h*)", 0)[1];
    var currentIndent = currentIndentSpaces / tabWidth;
    var lineCodeStart = lineStart + currentIndentSpaces;

    var toDelete, nextChar, runningOffset = 0, extraNewLine, currentToken, region, regionStart;

    if (debug) { ["view", view, "char", char, "mod", mod, "unicode", unicode, "keycode", keycode, "key", key].postcs; };

    suppressKeyPress = false;

    // Add matching character if necessary, or refrain from doubling
    matchChars.do { |arr|
      var beginChar = arr[0], endChar = arr[1];

      // don't double end char
      if (selectionSize == 0 && (char == endChar) && (char == view.string[selectionStart])) {
        // just move the selection forward
        view.select(selectionStart + 1, 0);
        ^true;
      };

      // match chars
      if (char == beginChar) {
        if (selectionSize == 0) {
          this.setString(beginChar ++ endChar, selectionStart, 0);
        } {
          view.selectedString = beginChar ++ view.selectedString ++ endChar;
        };
        this.colorize(false, selectionStart, selectionStart + selectionSize + 2);
        view.select(selectionStart + 1, selectionSize);
        ^true;
      };
    };

    // Delete matching character or tab, if necessary
    if (key == 16777219 && mod.isCmd.not) { // backspace
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
            this.setString("", selectionStart - 1, 2);
            ^true;
          };
        };

        this.setString("", selectionStart - 1, 1);
        this.colorize(false);
        ^true;
      };

      this.setString("", selectionStart, selectionSize);
      this.colorize(false);
      ^true;
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
        region = view.string.findRegexp(".+?$")
        .select({ |match| match[0] >= (lineStart - 1) && (match[0]  < lineEnd) });

        regionStart = region[0][0];
        if (region[0][1][0] == $\n) {
          region[0][1] = region[0][1][1..];
          regionStart = regionStart + 1;
        };

        region = region.collect({ |match|
          var line = match[1], currentIndentSpace, currentTabOffset;
          if (line[0] == $\n) { line = line[1..] };
          if (mod.isShift) {
            currentIndentSpaces = line.findRegexpAt("(\\s*)", 0)[1];
            currentTabOffset = currentIndentSpaces % tabWidth; // the amount off the tab grid the line is

            if (currentIndentSpaces > 0) {
              if (currentTabOffset == 0) {
                line = line[tabWidth..];
              } {
                line = line[currentTabOffset..];
              };
            };

            line;
          } {
            String.newFrom($ .dup(tabWidth)) ++ line;
          };
        }).join("\n");

        this.setString(region, lineStart, lineEnd - lineStart);
        this.colorize(false, lineStart, lineStart + region.size);
        this.select(lineStart, region.size);

        /* -------
        {
          ([0] ++ view.string.findAll($\n).collect(_ + 1)) // all lines..
          .select(_ >= lineStart).select(_ < lineEnd) //.. between selection
          .do { |lineStart, i|
            lineStart = lineStart + runningOffset; // account for previous indents

            runningOffset = runningOffset + if (mod.isShift) {
              this.deindentAt(lineStart);
            } {
              this.indentAt(lineStart);
            };

            if (i % 5 == 4) { 0.01.wait };

          };
        }.fork(AppClock);
        */
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
          this.setString("\n" ++ String.newFrom($ .dup(currentIndentSpaces)), selectionStart, selectionSize);
          view.select(selectionStart, 0);
        };
        this.setString("\n" ++ String.newFrom($ .dup(max(currentIndentSpaces + runningOffset, 0))), selectionStart, selectionSize);
      };

      ^true;
    };

    suppressKeyPress = true;

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

        if (currentToken[1] == \string) {
          HelpBrowser.openHelpFor("String");
        };

        if ([\number, \symbol/*, \string*/].indexOf(currentToken[1]).notNil) {
          HelpBrowser.goTo(SCDoc.helpTargetUrl +/+ "Reference/Literals.html#" ++ currentToken[1].asString.capitalize ++ "s");
          ^true;
        };

        if ([\comment, \longComment].indexOf(currentToken[1]).notNil) {
          HelpBrowser.goTo(SCDoc.helpTargetUrl +/+ "Reference/Comments.html");
          ^true;
        };
      };
    };

    if (key == 86 && mod.isCmd) { // paste
      paste = (start: selectionStart, stringSize: view.string.size - selectionSize);
    };

    if (key == 90 && mod.isCmd && mod.isAlt.not && mod.isShift.not) { // undo cmd-z
      this.undo;
      ^true;
    };
    if (key == 89 && mod.isCmd && mod.isAlt.not && mod.isShift.not) { // redo cmd-y
      this.redo;
      ^true;
    };

    if (key == 16777235 && mod.isCmd && mod.isAlt.not && mod.isShift.not) { // cmd-up
      this.changed(\cmdup);
      ^true;
    };

    if (key == 16777237 && mod.isCmd && mod.isAlt.not && mod.isShift.not) { // cmd-down
      this.changed(\cmddown);
      ^true;
    };

    if (key == 16777234 && mod.isCmd && mod.isAlt.not && mod.isShift.not) { // cmd-left
      this.changed(\cmdleft);
      ^true;
    };

    if (key == 16777236 && mod.isCmd && mod.isAlt.not && mod.isShift.not) { // cmd-right
      this.changed(\cmdright);
      ^true;
    };

    if (key == 32 && mod.isShift && mod.isAlt.not && mod.isCmd.not) { // shift-space
      this.changed(\shiftspace);
      ^true;
    };

    if (key == 66 && mod.isCmd && mod.isAlt.not) { // cmd-B
      if (mod.isShift) {
        Server.default.reboot;
        ^true;
      };
      Server.default.boot;
      ^true;
    };

    if (key == 75 && mod.isCmd && mod.isShift.not && mod.isAlt.not) { // cmd-K
      Server.default.quit;
      ^true;
    };

    if (key == 76 && mod.isCmd && mod.isShift && mod.isAlt.not) { // shift-cmd-L
      // Maybe add some sort of hook here? to confirm whether or not to recompile?
      // risk of losing unsaved changes....
      thisProcess.recompile;
      ^true;
    };

    if (key == 80 && mod.isCmd && mod.isShift && mod.isAlt.not) { // shift-cmd-P
      this.changed(\clearPost);
      ^true;
    };

    if (key == 77 && mod.isCmd && (mod.isShift && mod.isAlt).not) { // cmd-M
      if (mod.isShift) {
        Server.default.scope;
        ^true;
      };
      if (mod.isAlt) {
        Server.default.freqscope;
        ^true;
      };
      Server.default.meter;
      ^true;
    };

    if (key == 84 && mod.isCmd && (mod.isShift && mod.isAlt).not) { // cmd-T
      if (mod.isShift) {
        Server.default.queryAllNodes(true);
        PostView.postln("WARNING: Server dump only visible in IDE post window. Use alt-cmd-T for graphical view.")
        ^true
      };
      if (mod.isAlt) {
        Server.default.plotTree;
        ^true
      };
      Server.default.queryAllNodes;
      PostView.postln("WARNING: Server dump only visible in IDE post window. Use alt-cmd-T for graphical view.")
      ^true;
    };

    // don't handle modifiers or escape...
    if ((char.ascii < 32)) {
      //this.changed(\keyPressed)
      suppressKeyPress = false;
      ^modKeyHandler.(view, char, mod, unicode, keycode, key);
    };

    // ...otherwise insert character
    this.setString(char.asString, selectionStart, selectionSize);
    this.colorize(false);
    ^true;
  }
}
