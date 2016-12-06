CodeViewCompleteWindow : SCViewHolder {
  var <win, <codeView, <height;

  *new { |codeView, bounds|
    ^super.new.init(codeView, bounds);
  }

  init { |argcodeView, bounds|
    codeView = argcodeView;
    codeView.addDependant(this);

    bounds = bounds ?? Rect(0, 300, 300, 300);
    height = bounds.height;
    bounds = bounds.height_(1);

    win = Window("Auto Complete", bounds)
    .onClose_({
      codeView.removeDependant(this);
    })
    .background_(Color.gray(0.5, 0))
    .alwaysOnTop_(true)
    .front;

    view = ListView(win, bounds.copy.origin_(0@0))
    .resize_(5)
    .palette_(codeView.palette)
    .font_(codeView.font)
    .background_(codeView.palette.base.alpha_(0.9));
  }

  close {
    win.close;
  }

  showCompletions {
    if (win.bounds.height == 1) {
      win.bounds = win.bounds.height_(height).top_(win.bounds.top - height + 1);
    };
  }

  hideCompletions {
    if (win.bounds.height != 1) {
      height = win.bounds.height;
      win.bounds = win.bounds.height_(1).top_(win.bounds.top + height - 1);
      view.items = [];
    };
  }

  update { |obj, what|
    if (what == \escapePressed) {
      this.hideCompletions;
      ^false;
    };
    if (what == \keyPressed || (what == \mouseClicked)) {
      var pos = codeView.selectionStart - 1;
      var braceCount = 0;
      var str = codeView.string;

      var token = codeView.getTokenAtCursor(true);
      var type = token[1];
      var prevtoken = token[2];
      var prevtype = token[3];
      var complete, method, methods, class, items;
      token = token[0];

      // don't auto complete if there's a selection
      if (codeView.selectionSize > 0) {
        this.hideCompletions;
        ^false
      };

      if (type == \class) {
        this.showCompletions;
        complete = Class.allClasses.detect({ |class| class.name == token.asSymbol }).notNil;
        view.items = [token ++ if (complete) { "" } { " ..." }] ++ Class.allClasses.select({ |class|
          class.name.asString.beginsWith(token);
        }).collect({ |item|
          item.name;
        });
        ^true;
      };

      if (type == \envvar || (str[pos] == $~)) {
        this.showCompletions;
        items = [token ++ " ...", "In currentEnvironment:"];

        currentEnvironment.keys.asArray.select({ |item|
          if (token.size > 1) {
            item.asString.beginsWith(token[1..])
          } {
            true
          };
        }).sort.do { |envvar|
          items = items.add("   ~" ++ envvar ++ " -> " ++ currentEnvironment[envvar].asString);
        };

        view.items = items;
        ^true;
      };

      if ((type == \method || (str[pos] == $.)) && [\class, \envvar, \keyword, \number, \symbol, \string].indexOf(prevtype).notNil) {
        this.showCompletions;
        class = prevtoken.interpret.class;
        class = [class] ++ class.superclasses;
        view.items = [class[0].name ++ ":" ++ if (token == "" || (token == ".")) { "" } { token }  ++ " ..."] ++ class.collect({ |item| item.methods ?? [] }).flat.select({ |method|
          if (token.size == 0 || (token == ".")) { true } {
            method.name.asString.beginsWith(token);
          };
        }).collect({ |item| "   " ++ item.name ++ "(" ++ item.argNames.asArray.join(", ") ++ ")" });
        ^true;
      };

      if ((type == \method)) {
        this.showCompletions;
        if (token.size > 1) {
          methods = Class.allClasses.collect({ |item| item.methods ?? [] }).flat.select({
            |item| item.name.asString.beginsWith(token)
          });
          method = methods.collect({ |item|
            var count = methods.select({ |m| m.name == item.name });
            if (item.ownerClass.name.asString.beginsWith("Meta_")) {
              "   *"
            } {
              "    "
            } ++ item.name ++ " [" ++ if (count.size == 1) { count[0].ownerClass.name } { count.size } ++ "]";
          }).asSet.asArray.sort({ |a, b| a[4..] < b[4..] });
          view.items = ["." ++ token ++ " ..."] ++ method;
        } {
          if (token.size == 1) {
            methods = Class.allClasses.collect({ |item| item.methods ?? [] }).flat.select({
              |item| item.name.asString.size < 4 && (item.name.asString.beginsWith(token))
            });
            method = methods.collect({ |item|
              var count = methods.select({ |m| m.name == item.name });
              if (item.ownerClass.name.asString.beginsWith("Meta_")) {
                "   *"
              } {
                "    "
              } ++ item.name ++ " [" ++ if (count.size == 1) { count[0].ownerClass.name } { count.size } ++ "]";
            }).asSet.asArray.sort({ |a, b| a[4..] < b[4..] });
            view.items = ["." ++ token ++ " ..."] ++ method;
          } {
            view.items = ["." ++ token ++ " ..."];
          }
        };
        ^true;
      };

      while { (pos >= 0) && (braceCount < 1) } {
        var charAtPos = str[pos];
        if (charAtPos == $() { braceCount = braceCount + 1 };
        if (charAtPos == $)) { braceCount = braceCount - 1 };
        pos = pos - 1;
      };

      if (braceCount == 1) {
        token = codeView.getTokenAtCursor(true, pos);
        type = token[1];
        prevtoken = token[2];
        prevtype = token[3];
        token = token[0];

        if (type == \class) {
          this.showCompletions;
          method = token.asSymbol.asClass.class.findRespondingMethodFor(\new);
          view.items = [
            token ++ "(" ++ method.argNames[1..].join(", ") ++ ")"
          ] ++ method.argumentString.split($,).collect({ |item|
            "   " ++ item.stripWhiteSpace
          });
          ^true;
        };
        if (type == \method) {
          this.showCompletions;
          if ([\class, \envvar, \keyword, \number, \symbol, \string].indexOf(prevtype).notNil) {
            class = prevtoken.interpret.class;
            method = class.findRespondingMethodFor(token.asSymbol);
            view.items = if (method.argNames.notNil) {
              [class.name ++ ":" ++ token ++ "(" ++ method.argNames.asArray[1..].join(", ") ++ ")"] ++ (method.argumentString ?? "").split($,).collect({ |item| "   " ++ item.stripWhiteSpace });
            } {
              [class.name ++ ":" ++ token ++ "()", "   [ no arguments ]"];
            };
          } { // instance method
            class = Class.allClasses.select({ |item|
              try { item.findMethod(token.asSymbol).notNil } { false }
            });
            if (class.size == 1) {
              method = class[0].findMethod(token.asSymbol);
              view.items = [class[0].name ++ ":" ++ token ++ "(" ++ method.argNames.asArray[1..].join(", ") ++ ")"] ++ (method.argumentString ?? "   [ no arguments ]").split($,).collect({ |item| "   " ++ item.stripWhiteSpace });
            } {
              view.items = ["." ++ token] ++ class.collect({ |item|
                var method = item.findMethod(token.asSymbol);
                "   " ++ method.asString ++ "(" ++ method.argNames.asArray[1..].join(", ") ++ ")"
              });
            };
          };
          ^true;
        };
      };

      this.hideCompletions;
      ^false;
    };
  }
}
