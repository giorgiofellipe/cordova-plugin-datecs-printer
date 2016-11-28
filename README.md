# cordova-plugin-datecs-printer

The first thing that you must know is that the plugin is available through this variable `window.DatecsPrinter`.

So, there's a lot of functions that you can call to execute each operation and perform the printer actions, these are the most important ones (you can see all on [printer.js](www/printer.js) file):

_(every function accept at least two parameters, and they're the last ones: onSuccess function and onError function)_

- **listBluetoothDevices():** will give you a list of all the already previously paired bluetooth devices
- **connect(address):** this will establish the bluetooth connection with the selected printer (you need pass the address `attribute` of the selected device)
- **feedPaper(lines):** this will "print" blank lines
- **printText(text, charset):** will print the text respecting tags definition (charset encoding is ISO-8859-1 by default)
- **printImage(base64, width, height, alignment):** will print the image, the expected parameters are: 
  - 1- Base64 image
  - 2- Printing box's width (it does not resize the image)
  - 3- Printing box's height (it does not resize the image)
  - 4- Alignment code (you can find the codes [here](#alignment-codes))
- **printBarcode(barcodeType, barcodeData):** this will print a barcode accordingly to the given type and data (you can find the barcode types code [here](#barcode-types-code))

### Reported Working Printer Models

These models were reported as working as expected:

<sub>(if you notice another model please let us know by opening a issue and reporting)</sub>

- DATECS DPP 250
- DATECS DPP 350
- EPSON TM P80

### Example
```javascript
window.DatecsPrinter.listBluetoothDevices(
  function (devices) {
    window.DatecsPrinter.connect(devices[0].address, 
      function() {
        printSomeTestText();
      },
      function() {
        alert(JSON.stringify(error));
      }
    );
  },
  function (error) {
    alert(JSON.stringify(error));
  }
);

function printSomeTestText() {
  window.DatecsPrinter.printText("Print Test!", 'ISO-8859-1', 
    function() {
      printMyImage();
    }
  );
}

function printMyImage() {
  var image = new Image();
  image.src = 'img/some_image.jpg';
  image.onload = function() {
      var canvas = document.createElement('canvas');
      canvas.height = 100;
      canvas.width = 100;
      var context = canvas.getContext('2d');
      context.drawImage(image, 0, 0);
      var imageData = canvas.toDataURL('image/jpeg').replace(/^data:image\/(png|jpg|jpeg);base64,/, ""); //remove mimetype
      window.DatecsPrinter.printImage(
          imageData, //base64
          canvas.width, 
          canvas.height, 
          1, 
          function() {
            printMyBarcode();
          },
          function(error) {
              alert(JSON.stringify(error));
          }
      )
  };
}

function printMyBarcode() {
  window.DatecsPrinter.printBarcode(
    75, //here goes the barcode type code
    '13132498746313210584982011487', //your barcode data
    function() {
      alert('success!');
    },
    function() {
      alert(JSON.stringify(error));
    }
  );
}
```

### Tags definition
- `{reset}`	    Reset to default settings.
- `{br}`	    Line break. Equivalent of new line.
- `{b}, {/b}`	Set or clear bold font style.
- `{u}, {/u}`	Set or clear underline font style.
- `{i}, {/i}`	Set or clear italic font style.
- `{s}, {/s}`	Set or clear small font style.
- `{h}, {/h}`	Set or clear high font style.
- `{w}, {/w}`	Set or clear wide font style.
- `{left}`	    Aligns text to the left paper edge.
- `{center}`	Aligns text to the center of paper.
- `{right}`	    Aligns text to the right paper edge.

### Alignment Codes
- CENTER = 1
- LEFT = 0
- RIGHT = 2

### Barcode Types Code
- UPC-A =	65
- UPC-E =	66
- EAN13 (JAN13) =	67
- EAN 8 (JAN8) = 68
- CODE 39 =	69
- ITF = 70
- CODABAR (NW-7) = 71
- CODE 93 = 72
- CODE 128 = 73
- PDF417 = 74
- CODE 128 Auto = 75
- EAN 128 = 76

## ConnectionStatus Event

To listen about the connection status this is the way you should go:
You should use this plugin to receive the broadcasts `cordova plugin add cordova-plugin-broadcaster`

```javascript
window.broadcaster.addEventListener( "DatecsPrinter.connectionStatus", function(e) {
  if (e.isConnected) {
    //do something
  }
});
```

## Angular / Ionic

If your intention is to use it with Angular or Ionic, you may take a look at this simple example: https://github.com/giorgiofellipe/cordova-plugin-datecsprinter-example.
There's a ready to use angular service implementation.
