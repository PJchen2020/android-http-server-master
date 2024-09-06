//var axios = require('./axios.min,js') ;

// 创建axios实例
const service = axios.create({
  baseURL: 'http://192.168.2.160:8080/', // api的base_url
  timeout: 5000 // 请求超时时间
});

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 可以在这里添加请求头等信息
    return config;
  },
  error => {
    // 请求错误处理
    console.log(error); // for debug
    Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 对响应数据做处理，例如只返回data部分
    const res = response;
    // 如果有错误码，则进行错误处理
    if (res.status !== 200) {
        console.log('res.code: ', res);
      // 可以在这里处理不同的错误信息
      console.log('Error: ', res.message);
      return Promise.reject(new Error(res.message || 'Error'));
    }
    return res.data;
  },
  error => {
    console.log('Error: ', error); // for debug
    return Promise.reject(error);
  }
);

//export default service;