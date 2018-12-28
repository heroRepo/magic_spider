/**
 * Created by xieqing on 2017/3/21.
 */
"use strict";
var page = require('webpage').create();
var system = require('system');
var defaultTimeout = 10000;
var navigated=false;
page.settings.loadImages=false;
page.settings.userAgent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36';
var settings = {
  loadImages:false,
  operation: "GET",
  headers: {
    "Accept":"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    "User-Agent":"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36"
  }
};
//var url = system.args[1];
//var url =decodeURIComponent(system.args[1]);
var json = eval('(' + decodeURIComponent(system.args[1]) + ')')
console.log(json);
var url = json.url

page.onResourceRequested = function (request, networkRequest) {
    if(request.url.indexOf(".css")>-1 || request.url.indexOf(".swf")>-1){
        //console.log('Request ' + JSON.stringify(request.url, undefined, 4));
        networkRequest.abort();
    }
};

page.onNavigationRequested = function(url, type, willNavigate, main) {
    //console.log("onNavigationRequested");
    //console.log(url);
  if(main && url.indexOf("http") >= 0){
    navigated = true;
  }
}

var outputResult = function(){
    console.log("outputResult");
    console.log("--clear--");
        var location = page.evaluate(function () {
            return window.location + "";
        });
        //console.log(location);

        var content = page.evaluate(function () {
            return document.getElementsByTagName('html')[0].outerHTML;
        });
        console.log(content);
}

var waitToExit = function(){
    console.log("waitToExit");
      if(navigated){
            navigated = false;
            setTimeout(waitToExit, 2000);
      }
      else{
        outputResult();
        phantom.exit();
      }
 };

function waitFor(testFx, onReady, timeOutMillis) {
    var maxtimeOutMillis = timeOutMillis ? timeOutMillis : defaultTimeout, //< Default Max Timout is 3s
        start = new Date().getTime(),
        interval = setInterval(function() {
            if ((new Date().getTime() - start > maxtimeOutMillis)) {
              console.log("--timeout--");
              phantom.exit();
            }
        }, 50); //< repeat check every 50ms
      testFx();
}

waitFor(function(){});

page.open(url, settings, function (status) {
        console.log(status);
        setTimeout(waitToExit, 5000);
});