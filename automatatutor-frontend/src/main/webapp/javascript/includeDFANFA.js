var Editor = {
  curConfigNfa: {
    dimensions: [740,480]
  },
  curConfigDfa: {
    dimensions: [740,480]
  }
};

function initCanvas() {
  if(!Editor.canvasNfa) {
      Editor.canvasNfa = new BlockCanvas("#svgcanvasnfa", Editor.curConfigNfa.dimensions, false, true, false);
  }
  if(!Editor.canvasDfa) {
      Editor.canvasDfa = new $.SvgCanvas("#svgcanvasdfa", Editor.curConfigDfa, 'powaut');
  }
}

$(document).ready(function() {
  initCanvas();
}); 


