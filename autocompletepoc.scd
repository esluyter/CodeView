var z, win, height = 300, autoCompleteBounds = Rect(0, height, 300, 1);

z = ListView(win = Window("Auto complete", autoCompleteBounds).front.background_(Color.gray(0.5, 0)).alwaysOnTop_(true), autoCompleteBounds.copy.origin_(0@0)).resize_(5).palette_(w.view.gui[\textBox].palette).font_(w.view.gui[\textBox].font).background_(w.view.gui[\textBox].palette.base.alpha_(0.9));

w.view.gui[\textBox].keyUpAction_({
  var pos = w.view.gui[\textBox].selectionStart - 1;
  var braceCount = 0;
  var str = w.view.gui[\textBox].string;
  var done = false;

  var makeAutoCompleteWindow = {
    if (win.bounds.height == 1) {
      win.bounds = win.bounds.height_(height).top_(win.bounds.top - height + 1);
    };
  };

  var token = w.view.gui[\textBox].getTokenAtCursor(true);
  var type = token[1];
  var prevtoken = token[2];
  var prevtype = token[3];
  var complete, method, methods, class;
  token = token[0];

  z.items = [];

  if (type == \class) {
    makeAutoCompleteWindow.();
    complete = Class.allClasses.detect({ |class| class.name == token.asSymbol }).notNil;
    z.items = [token ++ if (complete) { "" } { " ..." }] ++ Class.allClasses.select({ |class|
      class.name.asString.beginsWith(token);
    }).collect({ |item|
      item.name;
    });
    done = true;
  };

  if ((type == \method) && (prevtype == \class) && done.not) {
    makeAutoCompleteWindow.();
    class = prevtoken.asSymbol.asClass;
    class = [class] ++ class.superclasses;
    z.items = [prevtoken ++ "." ++ if (token == "") { "" } { token }  ++ " ..."] ++ class.collect({ |item| item.class.methods ?? [] }).flat.select({ |method|
      if (token.size == 0) { true } {
        method.name.asString.beginsWith(token);
      };
    }).collect({ |item| "   *" ++ item.name });
    done = true;
  };

  if ((type == \method) && done.not) {
    makeAutoCompleteWindow.();
    if (token.size > 0) {
      methods = Class.allClasses.collect({ |item| item.methods ?? [] }).flat.select({|item| item.name.asString.beginsWith(token)});
      method = methods.collect({ |item|
        var count = methods.select({ |m| m.name == item.name });
        if (item.ownerClass.name.asString.beginsWith("Meta_")) {
          "   *"
        } {
          "    "
        } ++ item.name ++ " [" ++ if (count.size == 1) { count[0].ownerClass.name } { count.size } ++ "]";
      }).asSet.asArray.sort({ |a, b| a[4..] < b[4..] });
      z.items = ["." ++ token ++ " ..."] ++ method;
    } {
      z.items = ["." ++ token ++ " ..."];
    };
    done = true;
  };

  while { (pos >= 0) && (braceCount < 1) && done.not } {
    var charAtPos = str[pos];
    if (charAtPos == $() { braceCount = braceCount + 1 };
    if (charAtPos == $)) { braceCount = braceCount - 1 };
    pos = pos - 1;
  };

  if (braceCount == 1) {
    makeAutoCompleteWindow.();
    token = w.view.gui[\textBox].getTokenAtCursor(true, pos);
    type = token[1];
    prevtoken = token[2];
    prevtype = token[3];
    token = token[0];

    if (type == \class) {
      z.items = token.asSymbol.asClass.class.findRespondingMethodFor(\new).argumentString.split($,).collect({ |item| "   " ++ item.stripWhiteSpace });
    };
    if (type == \method) {
      if (prevtype == \class) {
        method = prevtoken.asSymbol.asClass.class.findRespondingMethodFor(token.asSymbol);
        z.items = [prevtoken ++ "." ++ token ++ "(" ++ method.argNames[1..].join(", ") ++ ")"] ++ method.argumentString.split($,).collect({ |item| "   " ++ item.stripWhiteSpace });
      } { // instance method
        class = Class.allClasses.select({ |item|
          try { item.findMethod(token.asSymbol).notNil } { false }
        });
        if (class.size == 1) {
          method = class[0].findMethod(token.asSymbol);
          z.items = [class[0].name ++ ":" ++ token ++ "(" ++ method.argNames[1..].join(", ") ++ ")"] ++ method.argumentString.split($,).collect({ |item| "   " ++ item.stripWhiteSpace });
        } {
          z.items = ["[...]." ++ token] ++ class.collect({ |item|
            var method = item.findMethod(token.asSymbol);
            "   " ++ method.asString ++ "(" ++ if (method.argNames.notNil) { method.argNames[1..].join(", ") } { "" } ++ ")"
          });
        };
      };
    };

    done = true;
  };

  if (done.not) {
    if (win.bounds.height != 1) {
      height = win.bounds.height;
      win.bounds = win.bounds.height_(1).top_(win.bounds.top + height - 1);
    };
  };
})