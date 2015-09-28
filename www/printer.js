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
    exec(onSuccess, onError, 'Printer', 'teste');
  }
};

module.exports = printer;