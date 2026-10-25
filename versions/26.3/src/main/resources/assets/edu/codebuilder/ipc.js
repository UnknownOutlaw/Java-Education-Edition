/* Education Edition (Java) Code Builder: gives the MakeCode editor the connection Education's Code Builder gives it
   (window.ipcRenderer), carried to the game over a WebSocket to this computer. */
(function () {
  var token = "%TOKEN%";
  var listeners = {};
  var queue = [];
  var ws = null;
  var tries = 0;
  function emit(name) {
    var args = Array.prototype.slice.call(arguments, 1);
    (listeners[name] || []).slice().forEach(function (f) {
      try { f.apply(window.ipcRenderer, args); } catch (e) { console.error(e); }
    });
  }
  function connect() {
    ws = new WebSocket("ws://" + location.host + "/__edu/socket?token=" + token);
    ws.onopen = function () {
      tries = 0;
      var waiting = queue;
      queue = [];
      waiting.forEach(function (m) { ws.send(m); });
    };
    ws.onmessage = function (e) { emit("responseFromApp", {}, e.data); };
    ws.onclose = function () {
      ws = null;
      setTimeout(connect, Math.min(5000, 500 * ++tries));
    };
  }
  window.ipcRenderer = {
    on: function (name, f) { (listeners[name] = listeners[name] || []).push(f); return this; },
    once: function (name, f) {
      var self = this;
      function g() { self.removeListener(name, g); f.apply(self, arguments); }
      return this.on(name, g);
    },
    removeListener: function (name, f) {
      var l = listeners[name];
      if (l) { var i = l.indexOf(f); if (i >= 0) l.splice(i, 1); }
      return this;
    },
    sendToHost: function (channel, data) {
      var text = typeof data === "string" ? data : JSON.stringify(data);
      if (ws && ws.readyState === 1) ws.send(text); else queue.push(text);
    },
    responseFromApp: function (message, name) { emit(name || "responseFromApp", {}, message); },
    registerResetCallback: function () {},
    unregisterResetCallback: function () {}
  };
  connect();
})();
