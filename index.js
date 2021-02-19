
import { NativeModules,NativeEventEmitter } from 'react-native';
const {RNHDIdCard}=NativeModules;
const eventEmitter = new NativeEventEmitter(RNHDIdCard);

export default {
/**
 * 导出模块设置监听方法回调
 */
// start:function(){
//     RNHDIdCard.start();
// },
// stop:function(){
//     RNHDIdCard.stop();
// },
// readIDCard:function(){
//     RNHDIdCard.readIDCard();
// },
// readSiCard:function(){
//     RNHDIdCard.readSiCard();
// },
// on:function(fn){
//     eventEmitter.addListener('console', (event) => {
//        fn(event)
//     });
// },
// callback:function(fn){
//     eventEmitter.addListener('callback', (event) => {
//        fn(event)
//     });
// }

};