
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
          }

      }).catch(err=>{
          console.log(err)
      })
  }