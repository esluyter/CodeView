PostView : SCViewHolder {
  var <parent, firstTime = true, <colorScheme, <palette, <font, <tokens, <>mute = false;
  classvar <all;

  *post { |str|
    all.do(_.post(str));
  }
  *postln { |str|
    all.do(_.postln(str));
  }

  *new { |parent, bounds|
    if (Quarks.isInstalled("ddwStatusBox")) {
      "Installed quark ddwStatusBox conflicts with PostView extensions to String. Please pick one to use and uninstall the other.".warn;
    };
    ^super.new.init(parent, bounds);
  }
  init { |argparent, argbounds|
    parent = argparent;
    all = all.add(this);

    tokens = (
      success: "^\\->\\s.*?$",
      warning: "^WARNING:\\s.*?$",
      error: "^ERROR:\\s.*?$"
    );

    this.view = TextView(parent, argbounds)
    .editable_(false);

    this.onClose_({ all.remove(this) });

    this.font = Font.monospace.size_(11);
    this.darkColorScheme;
  }

  post { |str|
    var start = view.string.size - 1;
    if (mute) { ^false };
    view.setString(str, start, 0);
    view.select(view.string.size - 1, 0);
    this.colorize(start, str.size);
  }
  postln { |str|
    this.post(str.asString ++ "\n");
    if (firstTime && mute.not) { firstTime = false; this.post("\n") }; // need this to flush it all out
  }
  clear {
    view.string = "";
    firstTime = true;
  }

  font_ { |afont|
    font = afont;
    view.font = font;
  }
  colorScheme_ { |value|
    colorScheme = value;

    palette = QPalette.auto(colorScheme[\background], colorScheme[\background]);
		palette.base = colorScheme[\background];
    palette.baseText = colorScheme[\text];
    palette.highlight = colorScheme[\selectionBackground];
    palette.highlightText = colorScheme[\selectionText];

    view.palette = palette;
    this.colorize(0, view.string.size - 1);
  }
  darkColorScheme {
    this.colorScheme = (
      background: Color.new255(18, 8, 0).alpha_(0.9),
      text: Color.new255(247, 251, 239),
      selectionBackground: Color.new255(188, 210, 255),
      selectionText: Color.new255(42, 1, 0),
      success: Color.new255(135, 213, 0),
      warning: Color.new255(214, 163, 0),
      error: Color.new255(234, 43, 36)
    )
  }

  colorize { |start, size|
    var str = view.string[start..(start + size - 1)];

    view.setStringColor(colorScheme[\text], start, size);
    tokens.keysValuesDo { |type, regexp|
      str.findRegexp(regexp).do { |arr|
        var tokenStart, token;
        # tokenStart, token = arr;
        view.setStringColor(colorScheme[type], tokenStart + start, token.size);
      };
    };
  }
}
