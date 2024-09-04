function clickBluetoothTest() {
      axios.get('http://192.168.2.160:8080/api/bluetooth',{
        params:{
        action:"init"
        }
      }).then(res=>{
          console.log(res)
      }).catch(err=>{
          console.log(err)
      })
  }