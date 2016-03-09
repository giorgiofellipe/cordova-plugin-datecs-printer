# cordova-plugin-datecs-printer

The first thing that you must know is that the plugin is available through this variable `window.DatecsPrinter`.

So, there's a lot of functions that you can call to execute each operation and perform the printer actions, these are the most important ones (you can see all on [printer.js](www/printer.js) file):

_(every function accept at least two parameters, and they're the last ones: onSuccess function and onError function)_

- listBluetoothDevices(): will give you a list of all the already previously paired bluetooth devices
- connect(address): this will establish the bluetooth connection with the selected printer (you need pass the address `attribute` of the selected device)
- feedPaper(lines): this will "print" blank lines
- printText(text): will print the text respecting tags definition


```javascript
window.DatecsPrinter.listBluetoothDevices(
  function (devices) {
    window.DatecsPrinter.connect(devices[0].address, printSomeTestText);
  },
  function (error) {
  }
);

function printSomeTestText() {
  window.DatecsPrinter.printText("Print Test!");
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
