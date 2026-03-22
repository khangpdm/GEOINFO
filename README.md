# 🌍 ỨNG DỤNG TRA CỨU THÔNG TIN ĐỊA LÝ (JAVA SOCKET)

## 📌 Giới thiệu

Đây là đề tài môn **Lập trình mạng** xây dựng một hệ thống **Client–Server** bằng Java cho phép người dùng tra cứu thông tin địa lý (thành phố hoặc quốc gia) thông qua giao diện đồ họa (GUI).

Ứng dụng giúp cung cấp thông tin chi tiết như:

* Thông tin địa lý (tọa độ, quốc gia, dân số)
* Thời tiết hiện tại
* Tin tức liên quan
* Gợi ý khách sạn / địa điểm du lịch

---

## 🏗️ Kiến trúc hệ thống

Hệ thống gồm 2 thành phần chính:

### 🔹 Server

* Xử lý yêu cầu từ client
* Gọi API bên ngoài để lấy dữ liệu
* Mã hóa/giải mã dữ liệu
* Trả kết quả về client

### 🔹 Client (GUI)

* Nhập tên thành phố hoặc quốc gia
* Gửi yêu cầu đến server
* Hiển thị thông tin nhận được

---

## ⚙️ Công nghệ sử dụng

* **Ngôn ngữ:** Java
* **Giao thức:** TCP Socket
* **GUI:** 
* **API gợi ý:**

  * Weather Api (thời tiết)
  * REST Countries (quốc gia)

---

## 🔐 Bảo mật


---

## 🧩 Chức năng chi tiết

### 📍 1. Tra cứu Thành phố

Khi người dùng nhập tên **thành phố**, hệ thống trả về:

* Tọa độ (latitude, longitude)
* Quốc gia
* Dân số
* Thời tiết hiện tại
* Tin tức liên quan
* Danh sách khách sạn:

  * Tên khách sạn
  * Địa chỉ
  * Giá phòng
  * Đánh giá (review)

---

### 🌏 2. Tra cứu Quốc gia

Khi nhập **quốc gia**, hệ thống trả về:

* Thông tin cơ bản:

  * Thủ đô
  * Dân số
  * Diện tích
* Đơn vị tiền tệ
* Ngôn ngữ
* Quốc kỳ
* Các quốc gia láng giềng
* Thời tiết (thủ đô)
* Gợi ý:

  * Địa điểm du lịch nổi bật
  * Khách sạn tiêu biểu

---

## 🖥️ Giao diện người dùng (GUI)


---

## 🔄 Luồng hoạt động


---

## 📡 Yêu cầu hệ thống

* Java JDK 8 trở lên
* Kết nối Internet (để gọi API)
* Các client chạy trên **máy khác nhau**

---

## 📁 Cấu trúc thư mục

```
project/
│── server/
│   ├── Server.java
│   ├── ClientHandler.java
│   └── EncryptionUtil.java
│
│── client/
│   ├── ClientGUI.java
│   └── SocketClient.java
│
│── api/
│   └── ApiService.java
│
│── README.md
```

---

## 📌 Mở rộng (Optional)

* Cache dữ liệu để giảm gọi API
* Thêm bản đồ (Google Maps)
* Đăng nhập người dùng
* Lưu lịch sử tìm kiếm
* Triển khai đa luồng server

---

## 📜 License

Dự án phục vụ mục đích học tập.
