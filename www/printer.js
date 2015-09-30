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

  teste: function(onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'teste', []);
  },

  listBluetoothDevices: function(onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'listBluetoothDevices', []);
  },

  connect: function(address, onSuccess, onError) {
    exec(onSuccess, onError, 'DatecsPrinter', 'connect', [address]);
  }
};

module.exports = printer;