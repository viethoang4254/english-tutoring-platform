import styles from "../../app/public-pages.module.css";

// Owner-supplied public details; unrelated to SMTP delivery configuration.
export const contactInformation = {
  websiteName:"Tiếng Anh Cùng Gia Sư",
  address:"566 Núi Thành, phường Hòa Cường, thành phố Đà Nẵng, Việt Nam",
  phone:"0905700682", phoneHref:"tel:+84905700682",
  email:"viehoang21104@gmail.com",
  mapsUrl:"https://www.google.com/maps/place/566+N%C3%BAi+Th%C3%A0nh,+H%C3%B2a+C%C6%B0%E1%BB%9Dng,+%C4%90%C3%A0+N%E1%BA%B5ng,+Vi%E1%BB%87t+Nam/@16.0321534,108.219591,17z/data=!3m1!4b1!4m6!3m5!1s0x314219ee4554218d:0x7ff6c1a286a840!8m2!3d16.0321534!4d108.2221659!16s%2Fg%2F11wr155fsn?hl=vi&entry=ttu&g_ep=EgoyMDI2MTExNS4wIKXMDSoASAFQAw%3D%3D",
  mapEmbedUrl:"",
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
      (url.pathname==="/maps/embed" || url.pathname.startsWith("/maps/embed/")))embed=url.href;
  }catch{/* An absent or invalid embed keeps the address/link fallback. */}
  return <section className={`${styles.map} ${styles.card}`} aria-label="Vị trí liên hệ"><h2>Vị trí liên hệ</h2><p>{contactInformation.websiteName}</p><p>{address}</p>{embed&&<iframe src={embed} title="Bản đồ vị trí liên hệ" loading="lazy" referrerPolicy="no-referrer"/>}<div className={styles.actions}><a href={contactInformation.mapsUrl} target="_blank" rel="noopener noreferrer">Mở Google Maps<span> (thẻ mới)</span></a></div></section>;
}
