function onloadFunction() {
//    let blueStatus = { isBTSupport: true, isBTEnable: false }
//    let wifiStatus = { isWifiSupport: true, isWifiEnable: false }
//
//    if (!blueStatus["isBTEnable"]) {
//    let btEnableTr = document.getElementById("btEnableTr");
//    <!--        let btEnableResult = document.getElementById("btEnableResult");-->
//    <!--        btEnableResult.innerHTML = "Disable";-->
//    var btEnableButton = document.createElement("td");
//    btEnableButton.id = "btEnableButton"
//    btEnableButton.innerHTML = `<a onclick='enableBT()' href='#'>enable</a>`;
//    btEnableTr.appendChild(btEnableButton);
//    }
//
//    if (!wifiStatus["isWifiEnable"]) {
//    let wifiEnableTr = document.getElementById("wifiEnableTr");
//    console.log(wifiEnableTr);
//
//    let wifiEnableResult = document.getElementById("wifiEnableResult");
//    wifiEnableResult.innerHTML = "Disable";
//    var wifiEnableButton = document.createElement("td");
//    wifiEnableButton.id = "wifiEnableButton"
//    wifiEnableButton.innerHTML = `<a onclick='enableWifi()' href='#'>enable</a>`;
//    wifiEnableTr.appendChild(wifiEnableButton);
//    }
}


function clickBluetoothTest() {
      service.get('api/bluetooth',{
        params:{
        action:"init"
        }
      }).then(res=>{
          var btSupportResult = document.getElementById("btSupportResult");
          var btEnableResult = document.getElementById("btEnableResult");
          console.log(btSupportResult)

          if(res.isBluetoothSupported){
            btSupportResult.innerHTML = 'Pass';
          }else {
            btSupportResult.innerHTML = 'Fail';
          }

          if(res.isBluetoothEnabled){
              btEnableResult.innerHTML = 'Enable';
          }else {
              btEnableResult.innerHTML = 'Disable';
              let btEnableTr = document.getElementById("btEnableTr");
              var btEnableButton = document.createElement("td");
              btEnableButton.id = "btEnableButton"
              btEnableButton.innerHTML = `<button onclick='enableBT()'>enable</button>`;
              btEnableTr.appendChild(btEnableButton);
          }

      }).catch(err=>{
          console.log(err)
      })
  }

function enableBT() {
   service.get('api/bluetooth',{
          params:{
          action:"enable"
          }
        }).then(res=>{
            var btEnableResult = document.getElementById("btEnableResult");

            if(res.result=='Pass'){
              btEnableResult.innerHTML = 'Enable';
              let btEnableButton = document.getElementById("btEnableButton");
              btEnableButton.hidden = true
            }else {
              btEnableResult.innerHTML = 'Disable';
              let connectedText = document.getElementById("connectedBT");
              connectedText.style.color = "red"
              connectedText.innerHTML = `enable bluetooth fail, Please check if web tool have bluetooth permission`
            }
        }).catch(err=>{
            console.log(err)
        })
}

function clickBTSearch() {
    service.get('api/bluetooth',{
              params:{
              action:"search"
              }
            }).then(res=>{
            console.log(res.BtDeviceList)
                let btId
                let BTindex = 0;
                let bluetoothSearchResult = res.BtDeviceList
                let table = document.getElementById("BluetoothDevices");
                  table.className = "table";
                  for (var bluetooth of bluetoothSearchResult) {

                    btId = "bt" + BTindex;
                    var tr = document.createElement("tr");
                    tr.id = btId;
                    var tdDeviceName = document.createElement("td");
                    tdDeviceName.innerHTML = bluetooth["deviceName"];
                    var tdMac = document.createElement("td");
                    tdMac.innerHTML = bluetooth["address"];
                    var tdAction = document.createElement("td");
                    tdAction.innerHTML = `<button onclick='btConnect(${btId})'>connect</button>`;
                    let tdResult = document.createElement("td");
                    tdResult.hidden = true;

                    tr.appendChild(tdDeviceName);
                    tr.appendChild(tdMac);
                    tr.appendChild(tdAction);
                    tr.appendChild(tdResult);
                    table.appendChild(tr);
                    BTindex++
                  }
                  let searchDeviceResult = document.getElementById("searchBTDeviceResult");
                  searchDeviceResult.innerHTML = "Pass"
            }).catch(err=>{
                console.log(err)
                })
}

function btConnect(btId) {
console.log(btId);
console.log(btId.children[0]);
console.log(btId.children[1]);
console.log(btId.children[2]);
console.log(btId.children[3]);
//console.log(btId[1].innerHTML);
 service.get('api/bluetooth',{
              params:{
              action:"connect",
              address:btId.children[1].innerHTML
              }
            }).then(res=>{
                console.log(res);
              let connectDeviceResult = document.getElementById("connectBTDeviceResult");

              console.log(btId.children);
              let connectedText = document.getElementById("connectedBT");
              let result;
              if (res.result === "Pass") {
                connectedText.style.color = "green"
                pResult = "succeed"
                result = "Pass"
              } else {
                connectedText.style.color = "red"
                pResult = "fail"
                result = "Fail"
              }
              connectDeviceResult.innerHTML = result;
              connectedText.innerHTML = `Bluetooth: ${btId.children[0].innerHTML} connect ${res.result}!`
              console.log(connectedText);

            }).catch(err=>{
                console.log(err)
               })
}

function clickWifiTest() {
      service.get('api/wifi',{
        params:{
        action:"init"
        }
      }).then(res=>{
          var wifiSupportResult = document.getElementById("wifiSupportResult");
          var wifiEnableResult = document.getElementById("wifiEnableResult");
          console.log(wifiSupportResult)

          if(res.isWifiSupported){
            wifiSupportResult.innerHTML = 'Pass';
          }else {
            wifiSupportResult.innerHTML = 'Fail';
          }

          if(res.isWifiEnable){
              wifiEnableResult.innerHTML = 'Enable';
          }else {
              wifiEnableResult.innerHTML = 'Disable';
              let wifiEnableTr = document.getElementById("wifiEnableTr");
              var wifiEnableButton = document.createElement("td");
              wifiEnableButton.id = "wifiEnableButton"
              wifiEnableButton.innerHTML = `<button onclick='enableWifi()'>enable</button>`;
              wifiEnableTr.appendChild(wifiEnableButton);
          }
      }).catch(err=>{
          console.log(err)
      })
  }

  function clickWifiSearch() {
      service.get('api/wifi',{
                params:{
                action:"scan"
                }
              }).then(res=>{
              console.log('res------',res)
              console.log(res.wifiApList)
                  let wifiId;
                  let wifiIndex = 0;
                  let wifiSearchResult = res.wifiApList
                  let table = document.getElementById("WifiDevices");
                    table.className = "table";
                    for (var wifi of wifiSearchResult) {

                      wifiId = "bt" + wifiIndex;
                      var tr = document.createElement("tr");
                      tr.id = wifiId;
                      var tdDeviceName = document.createElement("td");
                      tdDeviceName.innerHTML = wifi["SSID"];
                      var tdMac = document.createElement("td");
                      tdMac.innerHTML = wifi["BSSID"];
                      var tdAction = document.createElement("td");
                      tdAction.innerHTML = `<button onclick='btConnect(${wifiId})'>connect</button>`;
                      let tdResult = document.createElement("td");
                      tdResult.hidden = true;

                      tr.appendChild(tdDeviceName);
                      tr.appendChild(tdMac);
                      tr.appendChild(tdAction);
                      tr.appendChild(tdResult);
                      table.appendChild(tr);
                      wifiIndex++
                    }
                    let searchDeviceResult = document.getElementById("searchWifiDeviceResult");
                    searchDeviceResult.innerHTML = "Pass"
              }).catch(err=>{
                  console.log(err)
                  })
  }

