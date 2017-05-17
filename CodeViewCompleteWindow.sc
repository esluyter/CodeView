CodeViewCompleteWindow : SCViewHolder {
  var <win, <codeView, <height, <>autoHide = true, hideRout, lastToken, lastSelectedMethod, <isFront = false,
  <>toFrontAction, <>endFrontAction, codeViewParentWindow, <listActions, completed, selected, colors, selectionOffset, rout;

  *new { |codeView, bounds, codeViewParentWindow|
    ^super.new.init(codeView, bounds, codeViewParentWindow);
  }

  init { |argcodeView, bounds, argcodeViewParentWindow, border = true|
    codeViewParentWindow = argcodeViewParentWindow;

    codeView = argcodeView;
    codeView.addDependant(this);

    bounds = bounds ?? Rect(0, 300, 300, 300);
    height = bounds.height;
    bounds = bounds.height_(1);

    win = Window("Auto Complete", bounds, border: border)
    .toFrontAction_({
      toFrontAction.();
      isFront = true;
      if (codeViewParentWindow.notNil) {
        codeViewParentWindow.visible_(false).front; // hack to avoid stealing focus
      };
    })
    .endFrontAction_({
      endFrontAction.();
      isFront = false;
    })
    .onClose_({
      codeView.removeDependant(this);
    })
    .background_(Color.gray(0.5, 0))
    .alwaysOnTop_(true)
    .front;

    view = ListView(win, bounds.copy.origin_(0@0))
    .resize_(5)
    .action_({
      listActions[view.value].();
      //codeViewParentWindow.front;
      codeView.focus;
    });

    this.makeStyle;
  }

  visible {
    ^win.visible;
  }
  visible_ { |bool|
    win.visible = bool;
  }

  close {
    win.close;
  }

  showCompletions {
    hideRout.stop;
    if (win.bounds.height == 1) {
      win.bounds = win.bounds.height_(height).top_(win.bounds.top - height + 1);
    };
    lastSelectedMethod = nil;
  }

  hideCompletions {
    view.items = [];
    if (win.bounds.height != 1 && autoHide) {
      hideRout.stop;
      hideRout = fork {
        0.5.wait;
        defer { this.forceHideCompletions };
      };
    };
  }

  forceHideCompletions {
    height = win.bounds.height;
    win.bounds = win.bounds.height_(1).top_(win.bounds.top + height - 1);
    lastSelectedMethod = nil;
  }

  showPathCompletions { |token, start|
    var str = token.reverse[1..].reverse[1..]; // remove quotes
    var path = PathName(str).pathOnly;
    var fileName, rawPath, rawPathUp;

    if (File.exists(path)) {
      this.showCompletions;
      path = PathName(path);
      fileName = PathName(str).fileName;
      rawPath = str.findRegexp(".*\\/")[0][1];
      try {
        rawPathUp = str.findRegexp("(.*\\/).*?\\/")[1][1];
      } {
        rawPathUp = "/" ++ path.allFolders.asArray.reverse[1..].reverse.join("/");
      };

      listActions = listActions ++ [
        { this.complete("\"" ++ rawPath ++ "\"", start, token.size, -1) },
        { this.complete("\"" ++ rawPathUp ++ "\"", start, token.size, -1) }
      ];

      view.items = [token ++ " ...", ".", ".."]
      ++
      path.folders.select({ |folderpath|
        if (fileName == "") { true } { folderpath.folderName.beginsWith(fileName) };
      }).collect({ |folderpath|
        listActions = listActions.add({ this.complete("\"" ++ rawPath ++ folderpath.folderName ++ "/\"", start, token.size, -1) });
        folderpath.folderName ++ "/"
      })
      ++
      path.files.select({ |filepath|
        if (fileName == "") { true } { filepath.fileName.beginsWith(fileName) };
      }).collect({ |filepath|
        listActions = listActions.add({ this.complete("\"" ++ rawPath ++ filepath.fileName ++ "\"", start, token.size, -1) });
        filepath.fileName
      });

      selectionOffset = 1;
      this.select(2);

      ^true;
    };
    ^false;
  }

  showEnvVarCompletions { |token, start|
    var complete, items, envir;

    this.showCompletions;

    if (currentEnvironment.isKindOf(Environment)) {
      envir = currentEnvironment; // for regular Environments
    };
    if (currentEnvironment.isKindOf(EnvironmentRedirect)) {
      envir = currentEnvironment.envir; // for ProxySpaces, etc.
    };
    if (envir.isNil) { // if it's something weird just return.
      this.hideCompletions;
      ^false;
    };
    complete = envir.keys.detect({ |item| item.asString == token[1..] }).notNil;

    items = [
      token ++ if (complete) { "" } { " ..." },
      "In currentEnvironment ("
        ++ if (currentEnvironment.class.asString[0].isVowel) { "an " } { "a " }
        ++ currentEnvironment.class.asString ++ "):"
    ];
    listActions = listActions.add(nil); // extra line...

    envir.keys.asArray.select({ |item|
      if (token.size > 1) {
        item.asString.beginsWith(token[1..])
      } {
        true
      };
    }).sort.do { |envvar|
      listActions = listActions.add({ this.complete("~" ++ envvar, start, token.size) });
      items = items.add("   ~" ++ envvar ++ " -> " ++ currentEnvironment[envvar].asString);
    };

    view.items = items;
    selectionOffset = 2;
    if (complete) {
      completed = 0;
    };
    this.select(0);

    ^true;
  }

  showClassCompletions { |token, start|
    var complete;

    this.showCompletions;

    complete = Class.allClasses.detect({ |class| class.name == token.asSymbol }).notNil;

    view.items = [token ++ if (complete) { "" } { " ..." }] ++ Class.allClasses.select({ |class|
      class.name.asString.beginsWith(token);
    }).collect({ |item|
      var helpClass = item.name;
      var helpText = SCDoc.documents["Classes/" ++ helpClass].summary;
      listActions = listActions.add({ this.complete(item.name, start, token.size) });
      "   " ++ item.name ++ " - " ++ helpText;
    });

    selectionOffset = 1;
    if (complete) {
      completed = 0;
    };
    this.select(0);

    ^true;
  }

  showMethodCompletionsForClass { |class, token, start|
    var complete;
    var self = this;

    self.showCompletions;

    complete = class.findRespondingMethodFor(token.asSymbol).notNil;
    class = [class] ++ class.superclasses;
    rout = {
      0.01.wait;
      view.items = [class[0].name ++ ":" ++ token  ++ if (complete) { "" } { " ..." }] ++ class.collect({ |item| item.methods ?? [] }).flat.select({ |method|
        if (token.size == 0) { true } {
          method.name.asString.beginsWith(token);
        };
      }).collect({ |item|
        var classMethod = item.ownerClass.name.asString.beginsWith("Meta_");
        var helpClass = if (classMethod) { item.ownerClass.name.asString[5..] } { item.ownerClass.name.asString };
        var helpMethod = (if (classMethod) { "*" } { "-" }) ++ item.name;
        var helpText = try { if (SCDoc.documents["Classes/" ++ helpClass].makeMethodList.collect(_.asSymbol).includes(("_" ++ helpMethod).asSymbol)) { SCDoc.getMethodDoc(helpClass, helpMethod).findChild(\METHODBODY).findChild(\PROSE).children.collect(_.text).join } };

        listActions = listActions.add({ // TODO : figure out why this doesn't show method completions right away

          self.complete(item.name ++ "()", start, token.size, -1);
        });
        if (item.ownerClass.name.asString.beginsWith("Meta_")) {
          "   *"
        } {
          "    "
        } ++ item.name ++ "(" ++ item.argNames.asArray[1..].join(", ") ++ ")" ++ if (helpText.notNil) { " - " ++ helpText } { "" }
      });

      selectionOffset = 1;
      if (complete) {
        completed = 0;
      };
      self.select(0);
    }.fork(AppClock);

    ^true;
  }

  showMethodCompletions { |token, start|
    var method, methods, complete = false;

    this.showCompletions;

    // for speed, look up only small methods for small token input
    if (token.size < 3) {
      methods = Class.allClasses.collect({ |item| item.methods ?? [] }).flat.select({
        |item| item.name.asString.size < (token.size + 3) && (item.name.asString.beginsWith(token))
      });
    } {
      methods = Class.allClasses.collect({ |item| item.methods ?? [] }).flat.select({
        |item| item.name.asString.beginsWith(token)
      });
    };

    method = methods.collect({ |item|
      if (item.ownerClass.name.asString.beginsWith("Meta_")) {
        "   *"
      } {
        "    "
      } ++ item.name;
    }).asSet.asArray.sort({ |a, b| a[4..] < b[4..] });

    method = method.collect { |item|
      var count = methods.select({ |m| m.name == item[4..].asSymbol });

      if (item[4..] == token) { complete = true };

      listActions = listActions.add({ // TODO : figure out why this doesn't open up completions for method
        this.complete(item[4..] ++ "()", start, token.size, -1)
      });
      item ++ " [" ++ if (count.size == 1) {
        var method = count[0];
        var classMethod = method.ownerClass.name.asString.beginsWith("Meta_");
        var helpClass = if (classMethod) { method.ownerClass.name.asString[5..] } { method.ownerClass.name.asString };
        var helpMethod = (if (classMethod) { "*" } { "-" }) ++ method.name;
        var helpText = try { if (SCDoc.documents["Classes/" ++ helpClass].makeMethodList.collect(_.asSymbol).includes(("_" ++ helpMethod).asSymbol)) { SCDoc.getMethodDoc(helpClass, helpMethod).findChild(\METHODBODY).findChild(\PROSE).children.collect(_.text).join } };

        method.ownerClass.name  ++ "]" ++ if (helpText.notNil) { " - " ++ helpText } { "" }
      } { "(" ++ count.size.asString ++ "): " ++ count.collect({ |item| item.ownerClass.name.asString }).join(", ") ++ "]" };
    };

    view.items = ["." ++ token ++ if (complete) { "" } { " ..." }] ++ method;

    selectionOffset = 1;
    this.select(0);

    ^true;
  }

  showClassNewArguments { |token|
    var method;
    var helpClass = token;
    var helpText = SCDoc.documents["Classes/" ++ helpClass].summary;

    this.showCompletions;
    method = token.asSymbol.asClass.class.findRespondingMethodFor(\new);
    view.items = [
      token ++ "(" ++ method.argNames[1..].join(", ") ++ ")",
      helpText
    ] ++ method.argumentString.split($,).collect({ |item|
      "   " ++ item.stripWhiteSpace
    });
    ^true;
  }

  showMethodArguments { |class, token|
    var method;
    var classMethod = class.asString.beginsWith("Meta_");
    var helpClass = if (classMethod) { class.asString[5..] } { class.asString };
    var helpMethod = if (classMethod) { "*" ++ token } { "-" ++ token };
    var helpNode = SCDoc.getMethodDoc(helpClass, helpMethod).findChild(\METHODBODY).findChild(\PROSE);
    var helpText = if (helpNode.notNil) { helpNode.children.collect(_.text).join } { SCDoc.documents["Classes/" ++ helpClass].summary };

    this.showCompletions;
    method = class.findRespondingMethodFor(token.asSymbol);

    if (method.isNil) {
      this.hideCompletions;
      ^false;
    };

    view.items = if (method.argNames.notNil) {
      [
        class.name ++ ":" ++ token ++ "(" ++ method.argNames.asArray[1..].join(", ") ++ ")",
        helpText
      ] ++ (method.argumentString ?? "").split($,).collect({ |item|
        "   " ++ item.stripWhiteSpace
      });
    } {
      if (method.name.asString.endsWith("_")) {
        [class.name ++ ":" ++ token ++ "(value)", "   value"]
      } {
        [class.name ++ ":" ++ token ++ "()", ""];
      };
    };
    ^true;
  }

  selectClassForMethod { |possibleClasses, token|
    this.showCompletions;

    view.items = ["." ++ token] ++ possibleClasses.collect({ |item|
      var method = item.findMethod(token.asSymbol);

      var classMethod = method.ownerClass.name.asString.beginsWith("Meta_");
      var helpClass = if (classMethod) { method.ownerClass.name.asString[5..] } { method.ownerClass.name.asString };
      var helpMethod = (if (classMethod) { "*" } { "-" }) ++ method.name;
      var helpText = try { if (SCDoc.documents["Classes/" ++ helpClass].makeMethodList.collect(_.asSymbol).includes(("_" ++ helpMethod).asSymbol)) { SCDoc.getMethodDoc(helpClass, helpMethod).findChild(\METHODBODY).findChild(\PROSE).children.collect(_.text).join } };

      listActions = listActions.add({
        var class = method.ownerClass;
        lastSelectedMethod = method.name;
        listActions = [nil];
        this.showMethodArguments(class, method.name);
      });

      "   " ++ method.asString ++ "(" ++ method.argNames.asArray[1..].join(", ") ++ ")" ++ if (helpText.notNil) { " - " ++ helpText } { "" }
    });
    selectionOffset = 1;
    this.select(0);

    ^true;
  }

  testBrackets { |pos, openChr = $(, closeChr = $)|
    var str = codeView.string;
    var braceCount = 0;
    var token, type, start, prevtoken, prevtype;
    var class, classes;

    while { (pos >= 0) && (braceCount < 1) } {
      var charAtPos = str[pos];
      if (charAtPos == openChr) { braceCount = braceCount + 1 };
      if (charAtPos == closeChr) { braceCount = braceCount - 1 };
      pos = pos - 1;
    };

    if (braceCount == 1) {
      token = codeView.getTokenAtCursor(true, pos, getStart:true);
      if (token[1] == \punctuation) {
        token = codeView.getTokenAtCursor(true, token[5], getStart:true);
      };

      type = token[1];
      start = token[2];
      prevtoken = token[3];
      prevtype = token[4];
      token = token[0];

      if (type == \class) {
        ^this.showClassNewArguments(token);
      };

      if (type == \method) {
        if ([\class, \envvar, \keyword, \number, \symbol, \string].indexOf(prevtype).notNil) { // known class
          class = prevtoken.interpret.class;
          ^this.showMethodArguments(class, token);
        };

        // unknown class
        classes = Class.allClasses.select({ |item|
          try { item.findMethod(token.asSymbol).notNil } { false }
        });

        if (classes.size == 1) { // known class by process of elimination (only one class with that method)
          ^this.showMethodArguments(classes[0], token);
        };

        // otherwise, let user select the class
        //...unless they've already done so....
        if (token.asSymbol == lastSelectedMethod) {
          ^false;
        };
        ^this.selectClassForMethod(classes, token);
      };
    };

    ^nil;
  }

  complete { |string, start, size, cursorOffset = 0|
    string = string.asString;
    codeView.setString(string, start, size);
    codeView.select((start + string.size + cursorOffset), 0);
  }

  select { |index|
    var colors = Color.clear.dup(listActions.size);
    selected = index;
    if (selected + selectionOffset < listActions.size) {
      colors[selectionOffset + selected] = view.background.complementary.blend(Color.yellow).alpha_(0.25);
    };
    if (completed.notNil) {
      colors[selectionOffset + completed] = view.background.complementary.blend(Color.green).alpha_(0.2);
    };
    view.colors = colors;
  }

  makeStyle {
    view.palette_(codeView.palette)
    .font_(codeView.font.copy.size_(codeView.font.size * 0.8))
    .background_(codeView.palette.base.alpha_(0.9))
    .selectedStringColor_(Color.gray(codeView.palette.baseText.asHSV[2].round))
    .hiliteColor_(codeView.palette.base.blend(codeView.palette.base.complementary, 0.2));
  }

  update { |obj, what|
    if (what == \colorScheme) {
      this.makeStyle;
      ^false;
    };

    if (what == \escapePressed) {
      this.forceHideCompletions;
      ^false;
    };

    if (what == \cmdup) {
      this.select((selected - 1) % (listActions.size - selectionOffset));
    };
    if (what == \cmddown) {
      this.select((selected + 1) % (listActions.size - selectionOffset));
    };
    if (what == \cmdleft || (what == \cmdright) || (what == \shiftspace)) {
      listActions[selected + selectionOffset].();
    };

    if (what == \keyPressed || (what == \mouseClicked) || (what == \textInserted)) {
      var pos = codeView.selectionStart - 1;
      var braceCount = 0;
      var str = codeView.string;

      var token = codeView.getTokenAtCursor(true, getStart: true);
      var type = token[1];
      var start = token[2];
      var prevtoken = token[3];
      var prevtype = token[4];
      var complete = false, method, methods, class, items;
      token = token[0];

      // don't do anything if the token is the same
      if (token == lastToken && (what != \textInserted)) {
        ^false;
      };
      lastToken = token;

      // reset list actions
      listActions = [nil];
      selected = 0;
      completed = nil;
      rout.stop; rout = nil;

      // don't auto complete if there's a selection
      if (codeView.selectionSize > 0) {
        this.hideCompletions;
        ^false
      };

      // complete string path names
      if (type == \string) {
        ^this.showPathCompletions(token, start);
      };

      // complete environment vars in currentEnvironment
      if (type == \envvar || (str[pos] == $~)) {
        ^this.showEnvVarCompletions(token, start);
      };

      // complete classes
      if (type == \class) {
        ^this.showClassCompletions(token, start);
      };

      // complete methods on known classes
      if ((type == \method || (str[pos] == $.)) && [\class, \envvar, \keyword, \number, \symbol, \string].indexOf(prevtype).notNil) {
        class = prevtoken.interpret.class;
        if (token == ".") {
          token = "";
          start = start + 1;
        };
        ^this.showMethodCompletionsForClass(class, token, start);
      };

      // complete methods on unknown classes
      if ((type == \method)) {
        if (token.size > 0) {
          ^this.showMethodCompletions(token, start);
        };
      };

      if (this.testBrackets(pos).notNil) {
        ^true;
      };

      if (this.testBrackets(pos, ${, $}).notNil) {
        ^true;
      };

      this.hideCompletions;
      ^false;
    };
  }
}
