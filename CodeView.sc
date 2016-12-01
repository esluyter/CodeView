CodeView : SCViewHolder {
  var <parent, <font, <>matchChars;

  *new { |parent, bounds|
    ^super.new.init(parent, bounds);
  }

  init { |argparent, argbounds|
    parent = argparent;

    font = Font.monospace;
    matchChars = [
      [$", $"],
      [$', $'],
      [$[, $]],
      [$(, $)],
      [${, $}]
    ];

    this.view = TextView(parent, argbounds)
    .enterInterpretsSelection_(false)
    .font_(font)
    .keyDownAction_({ |view, char, mod, unicode, keycode, key|
      var ret = nil;

      var selectionStart = view.selectionStart;
      var selectionSize = view.selectionSize;

      var stringForward = view.string[(selectionStart + selectionSize)..];
      var selectedString = view.selectedString;
      var stringBackward = view.string[..(selectionStart - 1)].reverse;
      var cursorOnFirstLine = stringBackward.find($\n).isNil;
      var cursorOnLastLine = stringForward.find($\n).isNil;

      var lineStart = if (cursorOnFirstLine) { 0 } {
        selectionStart - stringBackward.find($\n);
      };
      var lineEnd = if (cursorOnLastLine) {
        selectionStart + stringForward.size;
      } {
        selectionStart + stringForward.find($\n);
      };

      var lineSize = lineEnd - lineStart;

      var toInterpret, toDelete, nextChar;

      key.postln;
      //[view, char, mod, unicode, keycode, key].postln;

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
          ret = true;
        };
      };

      // Delete matching character if necessary
      if (key == 16777219) { // backspace
        if (selectionSize == 0) {
          toDelete = view.string[selectionStart - 1];
          nextChar = view.string[selectionStart];
          matchChars.do { |arr|
            var beginChar = arr[0], endChar = arr[1];
            if ((toDelete == beginChar) && (nextChar == endChar)) {
              view.setString("", selectionStart - 1, 2);
              ret = true;
            };
          };
        };
      };

      // Up on top line goes to beginning
      if ((key == 16777235) && cursorOnFirstLine) { // up
        view.select(0, if (mod.isShift) { selectionStart + selectionSize } { 0 });
        ret = true;
      };

      // Down on bottom line goes to end
      if ((key == 16777237) && cursorOnLastLine && mod.isShift.not) { // down
        view.select(view.string.size, 0);
        ret = true;
      };

      // Execute code if necessary
      if ((key == 16777220) && (mod.isCmd || mod.isShift)) { // shift-enter & cmd-enter
        //"hi".postln;
        if (selectionSize != 0) {
          toInterpret = view.selectedString;
        } {
          if (mod.isShift) {
            toInterpret = view.currentLine;
            view.select(lineStart, lineSize);
          } {
            toInterpret = view.string;
            view.select(0, view.string.size);
          };

          { view.select(selectionStart, selectionSize) }.defer(0.1);
        };

        if (toInterpret.stripWhiteSpace != "") {
          ("-> " ++ toInterpret.interpret).postln;
        };

        ret = true; // don't do c++ evaluation
      };

      ret;
    });
  }
}
