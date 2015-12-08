var exec = require('cordova/exec');

var printer = {
  platforms: ['android'],

  isSupported: function() {
    if (window.device) {
      var platform = window.device.platform;
      if ((platform !== undefined) && (platform !== null)) {
        return (this.platforms.indexOf(platform.toLowerCase()) >= 0);
      }
    }
    return false;
  },

  listBluetoothDevices: function(onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'listBluetoothDevices', []);
  },
  connect: function(address, onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'connect', [address]);
  },
  disconnect: function(onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'disconnect', []);
  },
  feedPaper: function(lines, onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'feedPaper', [lines]);
  },
  printText: function(text, charset, onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'printText', [text, charset]);
  },
  printSelfTest: function (onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'printSelfTest', []);
  },
  getStatus: function (onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'getStatus', []);
  },
  getTemperature: function (onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'getTemperature', []);
  },
  setBarcode: function (align, small, scale, hri, height, onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'setBarcode', [align, small, scale, hri, height]);
  },
  printBarcode: function (type, data, onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'printBarcode', [type, data]);
  },
  printImage: function (image, width, height, align, onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'printImage', [image, width, height, align]);
  }
};
module.exports = printer;