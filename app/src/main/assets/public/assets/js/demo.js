
function clickBluetoothTest() {
      service.get('api/bluetooth',{
        params:{
        action:"init"
        }
      }).then(res=>{
          var btSupportResult = document.getElementById("btSupportResult");
          var btEnableResult = document.getElementById("btEnableResult");
          let searchButton = document.getElementById("btSearchButton");
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
              searchButton.disabled = true;
              let btEnableTr = document.getElementById("btEnableTr");
              var btEnableButton = document.createElement("td");
              btEnableButton.id = "btEnableButtonTd"
              btEnableButton.innerHTML = `<button id ="btEnableButton"  onclick='enableBT()'>enable</button>`;
              btEnableTr.appendChild(btEnableButton);
          }

      }).catch(err=>{
          console.log(err)
      })
  }

function enableBT() {
   let btEnableButton = document.getElementById("btEnableButton");
   btEnableButton.innerHTML = "Enable...";
   btEnableButton.disabled = true;
   service.get('api/bluetooth',{
          params:{
          action:"enable"
          }
        }).then(res=>{
            var btEnableResult = document.getElementById("btEnableResult");
            let searchButton = document.getElementById("btSearchButton");

            if(res.result=='Pass'){
              btEnableResult.innerHTML = 'Enable';
              searchButton.disabled = false;
              btEnableButton.hidden = true
            }else {
              btEnableResult.innerHTML = 'Disable';
              let connectedText = document.getElementById("connectedBT");
              connectedText.style.color = "red"
              btEnableButton.disabled = false;
              btEnableButton.innerHTML = "Enable";
              connectedText.innerHTML = `enable bluetooth fail, Please check if web tool have bluetooth permission`
            }
        }).catch(err=>{
            console.log(err)
            btEnableButton.innerHTML = "Enable";
            btEnableButton.disabled = false;
        })
}

function clickBTSearch() {
    let connectedText = document.getElementById("connectedBT");
    let searchButton = document.getElementById("btSearchButton");

    connectedText.style.color = "black"
    connectedText.innerHTML = `Searching devices, it will take about 15 second`
    searchButton.innerHTML = "Searching...";
    searchButton.disabled = true;
    service.get('api/bluetooth',{
              params:{
              action:"search"
              }
            }).then(res=>{
            searchButton.innerHTML = "Search Bluetooth"
            searchButton.disabled = false;
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
                  connectedText.innerHTML = `Search devices finish`
            }).catch(err=>{
                searchButton.innerHTML = "Search Bluetooth"
                searchButton.disabled = false;
                connectedText.innerHTML = `Search devices fail`
                })
}

function btConnect(btId) {
console.log(btId);
console.log(btId.children[0]);
console.log(btId.children[1]);
console.log(btId.children[2].children[0]);
console.log(btId.children[3]);

let connectedText = document.getElementById("connectedBT");
let connectDeviceResult = document.getElementById("connectBTDeviceResult");
btId.children[2].children[0].innerHTML = "connecting..."

connectedText.style.color = "black"
connectedText.innerHTML = `Connecting to ${btId.children[0].innerHTML}, check if need to pair devices`
 service.get('api/bluetooth',{
              params:{
              action:"connect",
              address:btId.children[1].innerHTML
              }
            }).then(res=>{
              btId.children[2].children[0].innerHTML = "connect"


              console.log(btId.children);

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
                btId.children[2].children[0].innerHTML = "connect"
                connectedText.innerHTML = `Bluetooth: ${btId.children[0].innerHTML} connect Fail!`
                console.log(err)
               })
}

function clickWifiTest() {
    let searchButton = document.getElementById("wifiSearchButton");
    let showIP = document.getElementById("showIP");
    searchButton.disabled = true;
      service.get('api/wifi',{
        params:{
        action:"init"
        }
      }).then(res=>{
          showIP.innerHTML = `devices IP is ${res.localIP}`
          var wifiSupportResult = document.getElementById("wifiSupportResult");
          var wifiEnableResult = document.getElementById("wifiEnableResult");
          console.log(wifiSupportResult)
          searchButton.disabled = false;

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
          searchButton.disabled = false;
          console.log(err)
      })
  }

  function clickWifiSearch() {
      let connectedText = document.getElementById("connectedWifi");
      let searchButton = document.getElementById("wifiSearchButton");
      let searchDeviceResult = document.getElementById("searchWifiDeviceResult");

      connectedText.style.color = "black"
      connectedText.innerHTML = `Searching wifi, it will take about 5 second`
      searchButton.innerHTML = "Searching...";
      searchButton.disabled = true;

      service.get('api/wifi',{
                params:{
                action:"scan"
                }
              }).then(res=>{
              searchButton.innerHTML = "Search Wifi";
              searchButton.disabled = false;
              connectedText.innerHTML = `Searching wifi finish`
              if(res.wifiApListCount===0){
                connectedText.innerHTML = `Can not found wifi, Please try a new search in a few minutes. `
                return;
              }

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
//                      var tdAction = document.createElement("td");
//                      tdAction.innerHTML = `<button onclick='btConnect(${wifiId})'>connect</button>`;
//                      let tdResult = document.createElement("td");
                      //tdResult.hidden = true;

                      tr.appendChild(tdDeviceName);
                      tr.appendChild(tdMac);
//                      tr.appendChild(tdAction);
                      //tr.appendChild(tdResult);
                      table.appendChild(tr);
                      wifiIndex++
                    }

                    searchDeviceResult.innerHTML = "Pass"
              }).catch(err=>{
                  searchButton.innerHTML = "Search Wifi";
                  searchButton.disabled = false;
                  connectedText.innerHTML = `Searching wifi finish`
                  console.log(err)
                  })
  }

