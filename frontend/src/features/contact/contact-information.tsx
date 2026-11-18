import styles from "../../app/public-pages.module.css";

// Owner-supplied public details; unrelated to SMTP delivery configuration.
export const contactInformation = {
  websiteName:"Tiếng Anh Cùng Gia Sư",
  address:"566 Núi Thành, phường Hòa Cường, thành phố Đà Nẵng, Việt Nam",
  phone:"0905700682", phoneHref:"tel:+84905700682",
  email:"viehoang21104@gmail.com",
  mapsUrl:"https://www.google.com/maps/place/566+N%C3%BAi+Th%C3%A0nh,+H%C3%B2a+C%C6%B0%E1%BB%9Dng,+%C4%90%C3%A0+N%E1%BA%B5ng,+Vi%E1%BB%87t+Nam/@16.0321534,108.219591,17z/data=!3m1!4b1!4m6!3m5!1s0x314219ee4554218d:0x7ff6c1a286a840!8m2!3d16.0321534!4d108.2221659!16s%2Fg%2F11wr155fsn?hl=vi&entry=ttu&g_ep=EgoyMDI2MTExNS4wIKXMDSoASAFQAw%3D%3D",
  mapEmbedUrl:"https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3834.639338406222!2d108.22228985!3d16.03227955!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x314219ee598df9c5%3A0x3109ad2f035819b5!2zNTY2IE7DumkgVGjDoG5oLCBIw7JhIEPGsOG7nW5nLCDEkMOgIE7hurVuZywgVmnhu4d0IE5hbQ!5e0!3m2!1svi!2s!4v1795005936915!5m2!1svi!2s",
};

export function ContactInformation(){
  const rows=[{label:"Địa chỉ",value:contactInformation.address,symbol:"⌖",href:undefined},{label:"Điện thoại",value:contactInformation.phone,symbol:"☎",href:contactInformation.phoneHref},{label:"Email",value:contactInformation.email,symbol:"@",href:`mailto:${contactInformation.email}`}].filter(row=>row.value.trim());
  return <dl className={styles.contactDetails}>{rows.map(row=><div key={row.label}><dt><span aria-hidden="true">{row.symbol}</span>{row.label}</dt><dd>{row.href?<a href={row.href}>{row.value}</a>:row.value}</dd></div>)}</dl>;
}

export function ContactMap({address,embedUrl}:{address:string;embedUrl:string}){
  if(!address.trim())return null;
  let embed:string|undefined;
  try{
    const url=new URL(embedUrl);
    if(url.origin==="https://www.google.com" && !url.username && !url.password &&
      (url.pathname==="/maps/embed" || url.pathname.startsWith("/maps/embed/")))embed=embedUrl;
  }catch{/* An absent or invalid embed keeps the address/link fallback. */}
  return <section className={`${styles.map} ${styles.card}`} aria-label="Vị trí liên hệ"><h2>Vị trí liên hệ</h2><p>{contactInformation.websiteName}</p><p>{address}</p>{embed&&<iframe src={embed} width="100%" title="Bản đồ địa chỉ 566 Núi Thành, Đà Nẵng" loading="lazy" allowFullScreen referrerPolicy="strict-origin-when-cross-origin"/>}<div className={styles.actions}><a href={contactInformation.mapsUrl} target="_blank" rel="noopener noreferrer">Mở Google Maps<span> (thẻ mới)</span></a></div></section>;
}
