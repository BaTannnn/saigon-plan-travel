# Chunking Qualitative Experiment

This offline experiment compares three practical chunking configurations on representative entries from the existing JSON corpus. Lengths are measured in characters with Python `len()`.

Current semantic-section distribution: minimum ≈ 263, median ≈ 366, p95 ≈ 433, maximum ≈ 480 characters. All current sections are below the production 800-character fallback threshold.

## Sample Places

- `dinh-doc-lap`
- `buu-dien-trung-tam-sai-gon`
- `bao-tang-my-thuat-tphcm`
- `cho-ben-thanh`
- `thao-cam-vien-sai-gon`
- `chua-ba-thien-hau`

## Manual Evaluation Rubric

Score each configuration manually. Do not infer these scores from chunk counts alone.

| Criterion | Recursive 500/50 | Recursive 800/100 | Markdown Header |
|---|---:|---:|---:|
| Avoids unnatural cuts | 1–3 | 1–3 | 1–3 |
| Topic coherence | 1–3 | 1–3 | 1–3 |
| Context preservation | 1–3 | 1–3 | 1–3 |
| Independently understandable | 1–3 | 1–3 | 1–3 |
| Reasonable chunk count/size | 1–3 | 1–3 | 1–3 |

Scale: **1 = poor, 2 = acceptable, 3 = good**.

## Place: dinh-doc-lap

### Configuration A — Recursive 500/50

Statistics: 4 chunks; min 288; average 295.2; max 303 characters.

#### Chunk 1

```text
# dinh-doc-lap

## OVERVIEW
Dinh Độc Lập là một di tích lịch sử và công trình kiến trúc tiêu biểu tại Thành phố Hồ Chí Minh. Di tích từng được biết đến qua nhiều tên gọi trong các giai đoạn lịch sử khác nhau và hiện là một địa điểm phục vụ hoạt động tham quan, tìm hiểu lịch sử và kiến trúc.
```

#### Chunk 2

```text
## BACKGROUND
Lịch sử khu vực Dinh Độc Lập bắt đầu từ Dinh Norodom được xây dựng trong thời kỳ Pháp thuộc. Sau nhiều biến động lịch sử và chính trị, công trình cũ được thay thế bằng Dinh Độc Lập mới trong thập niên 1960. Di tích gắn với nhiều sự kiện quan trọng của lịch sử Việt Nam trong thế kỷ XX.
```

#### Chunk 3

```text
## HIGHLIGHTS
Công trình Dinh Độc Lập hiện nay do kiến trúc sư Ngô Viết Thụ thiết kế. Kiến trúc của Dinh kết hợp ngôn ngữ hiện đại với những yếu tố và biểu tượng mang tinh thần kiến trúc truyền thống Á Đông. Không gian, đường nét và cách tổ chức mặt bằng tạo nên đặc điểm kiến trúc riêng của công trình.
```

#### Chunk 4

```text
## EXPERIENCE
Khách tham quan có thể tự khám phá Dinh Độc Lập thông qua hệ thống bảng thông tin, bảng chỉ dẫn và tên phòng tại di tích. Hình thức tham quan này cho phép du khách chủ động tìm hiểu các không gian của công trình và nội dung lịch sử được giới thiệu trong quá trình tham quan.
```

---

### Configuration B — Recursive 800/100

Statistics: 2 chunks; min 592; average 592.5; max 593 characters.

#### Chunk 1

```text
# dinh-doc-lap

## OVERVIEW
Dinh Độc Lập là một di tích lịch sử và công trình kiến trúc tiêu biểu tại Thành phố Hồ Chí Minh. Di tích từng được biết đến qua nhiều tên gọi trong các giai đoạn lịch sử khác nhau và hiện là một địa điểm phục vụ hoạt động tham quan, tìm hiểu lịch sử và kiến trúc.

## BACKGROUND
Lịch sử khu vực Dinh Độc Lập bắt đầu từ Dinh Norodom được xây dựng trong thời kỳ Pháp thuộc. Sau nhiều biến động lịch sử và chính trị, công trình cũ được thay thế bằng Dinh Độc Lập mới trong thập niên 1960. Di tích gắn với nhiều sự kiện quan trọng của lịch sử Việt Nam trong thế kỷ XX.
```

#### Chunk 2

```text
## HIGHLIGHTS
Công trình Dinh Độc Lập hiện nay do kiến trúc sư Ngô Viết Thụ thiết kế. Kiến trúc của Dinh kết hợp ngôn ngữ hiện đại với những yếu tố và biểu tượng mang tinh thần kiến trúc truyền thống Á Đông. Không gian, đường nét và cách tổ chức mặt bằng tạo nên đặc điểm kiến trúc riêng của công trình.

## EXPERIENCE
Khách tham quan có thể tự khám phá Dinh Độc Lập thông qua hệ thống bảng thông tin, bảng chỉ dẫn và tên phòng tại di tích. Hình thức tham quan này cho phép du khách chủ động tìm hiểu các không gian của công trình và nội dung lịch sử được giới thiệu trong quá trình tham quan.
```

---

### Configuration C — MarkdownHeader + Recursive 800/100

Statistics: 4 chunks; min 263; average 277.8; max 289 characters.

#### Chunk 1

Section: `OVERVIEW`

```text
Dinh Độc Lập là một di tích lịch sử và công trình kiến trúc tiêu biểu tại Thành phố Hồ Chí Minh. Di tích từng được biết đến qua nhiều tên gọi trong các giai đoạn lịch sử khác nhau và hiện là một địa điểm phục vụ hoạt động tham quan, tìm hiểu lịch sử và kiến trúc.
```

#### Chunk 2

Section: `BACKGROUND`

```text
Lịch sử khu vực Dinh Độc Lập bắt đầu từ Dinh Norodom được xây dựng trong thời kỳ Pháp thuộc. Sau nhiều biến động lịch sử và chính trị, công trình cũ được thay thế bằng Dinh Độc Lập mới trong thập niên 1960. Di tích gắn với nhiều sự kiện quan trọng của lịch sử Việt Nam trong thế kỷ XX.
```

#### Chunk 3

Section: `HIGHLIGHTS`

```text
Công trình Dinh Độc Lập hiện nay do kiến trúc sư Ngô Viết Thụ thiết kế. Kiến trúc của Dinh kết hợp ngôn ngữ hiện đại với những yếu tố và biểu tượng mang tinh thần kiến trúc truyền thống Á Đông. Không gian, đường nét và cách tổ chức mặt bằng tạo nên đặc điểm kiến trúc riêng của công trình.
```

#### Chunk 4

Section: `EXPERIENCE`

```text
Khách tham quan có thể tự khám phá Dinh Độc Lập thông qua hệ thống bảng thông tin, bảng chỉ dẫn và tên phòng tại di tích. Hình thức tham quan này cho phép du khách chủ động tìm hiểu các không gian của công trình và nội dung lịch sử được giới thiệu trong quá trình tham quan.
```

---

## Place: buu-dien-trung-tam-sai-gon

### Configuration A — Recursive 500/50

Statistics: 4 chunks; min 366; average 407.5; max 431 characters.

#### Chunk 1

```text
# buu-dien-trung-tam-sai-gon

## OVERVIEW
Bưu điện Trung tâm Sài Gòn là một công trình bưu chính và kiến trúc lịch sử nằm tại khu vực trung tâm Thành phố Hồ Chí Minh, cạnh Nhà thờ Đức Bà. Công trình được xây dựng vào cuối thế kỷ XIX và hiện vừa gắn với hoạt động bưu chính vừa là một điểm tham quan quen thuộc trong các chương trình khám phá khu trung tâm thành phố.
```

#### Chunk 2

```text
## BACKGROUND
Bưu điện Trung tâm Sài Gòn được xây dựng trong giai đoạn 1886-1891. Tư liệu của Sở Quy hoạch - Kiến trúc Thành phố Hồ Chí Minh ghi nhận công trình được thực hiện theo đồ án của kiến trúc sư Villedieu cùng các cộng sự trong thời kỳ thuộc địa. Qua hơn một thế kỷ, tòa nhà trở thành một trong những công trình kiến trúc lịch sử tiêu biểu còn hiện diện trong khu trung tâm Sài Gòn - Thành phố Hồ Chí Minh.
```

#### Chunk 3

```text
## HIGHLIGHTS
Giá trị nổi bật của Bưu điện Trung tâm Sài Gòn nằm ở hình thức kiến trúc cuối thế kỷ XIX và vị trí trong cụm công trình lịch sử ở trung tâm thành phố. Các tài liệu giới thiệu kiến trúc của thành phố xem Bưu điện Trung tâm là một trong những công trình nổi tiếng của Sài Gòn, có phong cách kiến trúc phương Tây kết hợp với các yếu tố phù hợp bối cảnh địa phương và tạo nên diện mạo đặc trưng của khu vực Công xã Paris.
```

#### Chunk 4

```text
## EXPERIENCE
Bưu điện Trung tâm Sài Gòn thường xuất hiện như một điểm dừng trong các chương trình tham quan khu trung tâm Thành phố Hồ Chí Minh. Khi đến đây, trải nghiệm chính là tiếp cận một công trình bưu chính vẫn gắn với đời sống đô thị đồng thời quan sát không gian kiến trúc lịch sử. Vị trí gần Nhà thờ Đức Bà và các điểm trung tâm khác cũng giúp địa điểm dễ kết hợp trong một hành trình đi bộ khám phá khu vực.
```

---

### Configuration B — Recursive 800/100

Statistics: 3 chunks; min 418; average 544.0; max 783 characters.

#### Chunk 1

```text
# buu-dien-trung-tam-sai-gon

## OVERVIEW
Bưu điện Trung tâm Sài Gòn là một công trình bưu chính và kiến trúc lịch sử nằm tại khu vực trung tâm Thành phố Hồ Chí Minh, cạnh Nhà thờ Đức Bà. Công trình được xây dựng vào cuối thế kỷ XIX và hiện vừa gắn với hoạt động bưu chính vừa là một điểm tham quan quen thuộc trong các chương trình khám phá khu trung tâm thành phố.

## BACKGROUND
Bưu điện Trung tâm Sài Gòn được xây dựng trong giai đoạn 1886-1891. Tư liệu của Sở Quy hoạch - Kiến trúc Thành phố Hồ Chí Minh ghi nhận công trình được thực hiện theo đồ án của kiến trúc sư Villedieu cùng các cộng sự trong thời kỳ thuộc địa. Qua hơn một thế kỷ, tòa nhà trở thành một trong những công trình kiến trúc lịch sử tiêu biểu còn hiện diện trong khu trung tâm Sài Gòn - Thành phố Hồ Chí Minh.
```

#### Chunk 2

```text
## HIGHLIGHTS
Giá trị nổi bật của Bưu điện Trung tâm Sài Gòn nằm ở hình thức kiến trúc cuối thế kỷ XIX và vị trí trong cụm công trình lịch sử ở trung tâm thành phố. Các tài liệu giới thiệu kiến trúc của thành phố xem Bưu điện Trung tâm là một trong những công trình nổi tiếng của Sài Gòn, có phong cách kiến trúc phương Tây kết hợp với các yếu tố phù hợp bối cảnh địa phương và tạo nên diện mạo đặc trưng của khu vực Công xã Paris.
```

#### Chunk 3

```text
## EXPERIENCE
Bưu điện Trung tâm Sài Gòn thường xuất hiện như một điểm dừng trong các chương trình tham quan khu trung tâm Thành phố Hồ Chí Minh. Khi đến đây, trải nghiệm chính là tiếp cận một công trình bưu chính vẫn gắn với đời sống đô thị đồng thời quan sát không gian kiến trúc lịch sử. Vị trí gần Nhà thờ Đức Bà và các điểm trung tâm khác cũng giúp địa điểm dễ kết hợp trong một hành trình đi bộ khám phá khu vực.
```

---

### Configuration C — MarkdownHeader + Recursive 800/100

Statistics: 4 chunks; min 324; average 386.5; max 417 characters.

#### Chunk 1

Section: `OVERVIEW`

```text
Bưu điện Trung tâm Sài Gòn là một công trình bưu chính và kiến trúc lịch sử nằm tại khu vực trung tâm Thành phố Hồ Chí Minh, cạnh Nhà thờ Đức Bà. Công trình được xây dựng vào cuối thế kỷ XIX và hiện vừa gắn với hoạt động bưu chính vừa là một điểm tham quan quen thuộc trong các chương trình khám phá khu trung tâm thành phố.
```

#### Chunk 2

Section: `BACKGROUND`

```text
Bưu điện Trung tâm Sài Gòn được xây dựng trong giai đoạn 1886-1891. Tư liệu của Sở Quy hoạch - Kiến trúc Thành phố Hồ Chí Minh ghi nhận công trình được thực hiện theo đồ án của kiến trúc sư Villedieu cùng các cộng sự trong thời kỳ thuộc địa. Qua hơn một thế kỷ, tòa nhà trở thành một trong những công trình kiến trúc lịch sử tiêu biểu còn hiện diện trong khu trung tâm Sài Gòn - Thành phố Hồ Chí Minh.
```

#### Chunk 3

Section: `HIGHLIGHTS`

```text
Giá trị nổi bật của Bưu điện Trung tâm Sài Gòn nằm ở hình thức kiến trúc cuối thế kỷ XIX và vị trí trong cụm công trình lịch sử ở trung tâm thành phố. Các tài liệu giới thiệu kiến trúc của thành phố xem Bưu điện Trung tâm là một trong những công trình nổi tiếng của Sài Gòn, có phong cách kiến trúc phương Tây kết hợp với các yếu tố phù hợp bối cảnh địa phương và tạo nên diện mạo đặc trưng của khu vực Công xã Paris.
```

#### Chunk 4

Section: `EXPERIENCE`

```text
Bưu điện Trung tâm Sài Gòn thường xuất hiện như một điểm dừng trong các chương trình tham quan khu trung tâm Thành phố Hồ Chí Minh. Khi đến đây, trải nghiệm chính là tiếp cận một công trình bưu chính vẫn gắn với đời sống đô thị đồng thời quan sát không gian kiến trúc lịch sử. Vị trí gần Nhà thờ Đức Bà và các điểm trung tâm khác cũng giúp địa điểm dễ kết hợp trong một hành trình đi bộ khám phá khu vực.
```

---

## Place: bao-tang-my-thuat-tphcm

### Configuration A — Recursive 500/50

Statistics: 4 chunks; min 411; average 447.8; max 494 characters.

#### Chunk 1

```text
# bao-tang-my-thuat-tphcm

## OVERVIEW
Bảo tàng Mỹ thuật Thành phố Hồ Chí Minh là một thiết chế văn hóa chuyên sưu tầm, bảo quản, trưng bày và giới thiệu các giá trị mỹ thuật. Bảo tàng được thành lập năm 1987 và chính thức mở cửa phục vụ công chúng vào năm 1989. Nội dung trưng bày bao quát cả mỹ thuật cổ - cận đại và mỹ thuật hiện đại, phản ánh nhiều giai đoạn và dòng chảy nghệ thuật của Thành phố Hồ Chí Minh, Nam Bộ và Việt Nam.
```

#### Chunk 2

```text
## BACKGROUND
Từ khi thành lập, Bảo tàng Mỹ thuật Thành phố Hồ Chí Minh tập trung xây dựng các bộ sưu tập và thực hiện chức năng bảo tồn, tôn vinh di sản mỹ thuật. Các sưu tập trải dài từ mỹ thuật cổ, cận đại đến hiện đại, trong đó có tác phẩm của nhiều thế hệ nghệ sĩ và những hiện vật phản ánh đặc trưng mỹ thuật Thành phố Hồ Chí Minh và khu vực Nam Bộ. Bên cạnh trưng bày thường xuyên, bảo tàng còn tổ chức các trưng bày chuyên đề để giới thiệu những lát cắt khác nhau của di sản nghệ thuật.
```

#### Chunk 3

```text
## HIGHLIGHTS
Không gian kiến trúc của Bảo tàng Mỹ thuật Thành phố Hồ Chí Minh mang dấu ấn kiến trúc Pháp cuối thế kỷ XIX - đầu thế kỷ XX và kết hợp các yếu tố trang trí phương Đông. Những chi tiết như mái ngói, cột ốp gốm và trang trí gốm trên mái tạo nên đặc trưng thẩm mỹ riêng cho khu nhà. Bản thân công trình vì vậy cũng là một phần đáng chú ý trong trải nghiệm tìm hiểu mỹ thuật và kiến trúc tại bảo tàng.
```

#### Chunk 4

```text
## EXPERIENCE
Khách tham quan có thể khám phá nhiều nhóm nội dung mỹ thuật trong các tòa nhà của bảo tàng. Nhà 1 tập trung vào mỹ thuật hiện đại, Nhà 2 dành cho trưng bày chuyên đề và triển lãm thường xuyên, còn Nhà 3 giới thiệu mỹ thuật cổ - cận đại với hiện vật từ nhiều chất liệu như gốm, đồng, gỗ và đá. Cách tổ chức này tạo điều kiện để người xem tiếp cận cả tác phẩm nghệ thuật, sưu tập chuyên đề và bối cảnh phát triển mỹ thuật qua nhiều thời kỳ.
```

---

### Configuration B — Recursive 800/100

Statistics: 4 chunks; min 411; average 447.8; max 494 characters.

#### Chunk 1

```text
# bao-tang-my-thuat-tphcm

## OVERVIEW
Bảo tàng Mỹ thuật Thành phố Hồ Chí Minh là một thiết chế văn hóa chuyên sưu tầm, bảo quản, trưng bày và giới thiệu các giá trị mỹ thuật. Bảo tàng được thành lập năm 1987 và chính thức mở cửa phục vụ công chúng vào năm 1989. Nội dung trưng bày bao quát cả mỹ thuật cổ - cận đại và mỹ thuật hiện đại, phản ánh nhiều giai đoạn và dòng chảy nghệ thuật của Thành phố Hồ Chí Minh, Nam Bộ và Việt Nam.
```

#### Chunk 2

```text
## BACKGROUND
Từ khi thành lập, Bảo tàng Mỹ thuật Thành phố Hồ Chí Minh tập trung xây dựng các bộ sưu tập và thực hiện chức năng bảo tồn, tôn vinh di sản mỹ thuật. Các sưu tập trải dài từ mỹ thuật cổ, cận đại đến hiện đại, trong đó có tác phẩm của nhiều thế hệ nghệ sĩ và những hiện vật phản ánh đặc trưng mỹ thuật Thành phố Hồ Chí Minh và khu vực Nam Bộ. Bên cạnh trưng bày thường xuyên, bảo tàng còn tổ chức các trưng bày chuyên đề để giới thiệu những lát cắt khác nhau của di sản nghệ thuật.
```

#### Chunk 3

```text
## HIGHLIGHTS
Không gian kiến trúc của Bảo tàng Mỹ thuật Thành phố Hồ Chí Minh mang dấu ấn kiến trúc Pháp cuối thế kỷ XIX - đầu thế kỷ XX và kết hợp các yếu tố trang trí phương Đông. Những chi tiết như mái ngói, cột ốp gốm và trang trí gốm trên mái tạo nên đặc trưng thẩm mỹ riêng cho khu nhà. Bản thân công trình vì vậy cũng là một phần đáng chú ý trong trải nghiệm tìm hiểu mỹ thuật và kiến trúc tại bảo tàng.
```

#### Chunk 4

```text
## EXPERIENCE
Khách tham quan có thể khám phá nhiều nhóm nội dung mỹ thuật trong các tòa nhà của bảo tàng. Nhà 1 tập trung vào mỹ thuật hiện đại, Nhà 2 dành cho trưng bày chuyên đề và triển lãm thường xuyên, còn Nhà 3 giới thiệu mỹ thuật cổ - cận đại với hiện vật từ nhiều chất liệu như gốm, đồng, gỗ và đá. Cách tổ chức này tạo điều kiện để người xem tiếp cận cả tác phẩm nghệ thuật, sưu tập chuyên đề và bối cảnh phát triển mỹ thuật qua nhiều thời kỳ.
```

---

### Configuration C — MarkdownHeader + Recursive 800/100

Statistics: 4 chunks; min 394; average 427.5; max 480 characters.

#### Chunk 1

Section: `OVERVIEW`

```text
Bảo tàng Mỹ thuật Thành phố Hồ Chí Minh là một thiết chế văn hóa chuyên sưu tầm, bảo quản, trưng bày và giới thiệu các giá trị mỹ thuật. Bảo tàng được thành lập năm 1987 và chính thức mở cửa phục vụ công chúng vào năm 1989. Nội dung trưng bày bao quát cả mỹ thuật cổ - cận đại và mỹ thuật hiện đại, phản ánh nhiều giai đoạn và dòng chảy nghệ thuật của Thành phố Hồ Chí Minh, Nam Bộ và Việt Nam.
```

#### Chunk 2

Section: `BACKGROUND`

```text
Từ khi thành lập, Bảo tàng Mỹ thuật Thành phố Hồ Chí Minh tập trung xây dựng các bộ sưu tập và thực hiện chức năng bảo tồn, tôn vinh di sản mỹ thuật. Các sưu tập trải dài từ mỹ thuật cổ, cận đại đến hiện đại, trong đó có tác phẩm của nhiều thế hệ nghệ sĩ và những hiện vật phản ánh đặc trưng mỹ thuật Thành phố Hồ Chí Minh và khu vực Nam Bộ. Bên cạnh trưng bày thường xuyên, bảo tàng còn tổ chức các trưng bày chuyên đề để giới thiệu những lát cắt khác nhau của di sản nghệ thuật.
```

#### Chunk 3

Section: `HIGHLIGHTS`

```text
Không gian kiến trúc của Bảo tàng Mỹ thuật Thành phố Hồ Chí Minh mang dấu ấn kiến trúc Pháp cuối thế kỷ XIX - đầu thế kỷ XX và kết hợp các yếu tố trang trí phương Đông. Những chi tiết như mái ngói, cột ốp gốm và trang trí gốm trên mái tạo nên đặc trưng thẩm mỹ riêng cho khu nhà. Bản thân công trình vì vậy cũng là một phần đáng chú ý trong trải nghiệm tìm hiểu mỹ thuật và kiến trúc tại bảo tàng.
```

#### Chunk 4

Section: `EXPERIENCE`

```text
Khách tham quan có thể khám phá nhiều nhóm nội dung mỹ thuật trong các tòa nhà của bảo tàng. Nhà 1 tập trung vào mỹ thuật hiện đại, Nhà 2 dành cho trưng bày chuyên đề và triển lãm thường xuyên, còn Nhà 3 giới thiệu mỹ thuật cổ - cận đại với hiện vật từ nhiều chất liệu như gốm, đồng, gỗ và đá. Cách tổ chức này tạo điều kiện để người xem tiếp cận cả tác phẩm nghệ thuật, sưu tập chuyên đề và bối cảnh phát triển mỹ thuật qua nhiều thời kỳ.
```

---

## Place: cho-ben-thanh

### Configuration A — Recursive 500/50

Statistics: 4 chunks; min 397; average 408.5; max 419 characters.

#### Chunk 1

```text
# cho-ben-thanh

## OVERVIEW
Chợ Bến Thành là một chợ truyền thống nằm ở khu vực trung tâm Thành phố Hồ Chí Minh và là một địa điểm quen thuộc trong hoạt động tham quan, mua sắm của du khách. Công trình chợ hiện nay có lịch sử hơn một thế kỷ và vẫn duy trì vai trò thương mại. Không gian chợ gắn với hình ảnh đô thị trung tâm, hoạt động buôn bán đa dạng và trải nghiệm văn hóa đời sống địa phương.
```

#### Chunk 2

```text
## BACKGROUND
Khu chợ hiện nay được hình thành từ chủ trương xây dựng một chợ trung tâm vào đầu thế kỷ XX. Công trình nhà lồng được khởi công trong giai đoạn 1913-1914 và lễ khai thị được tổ chức năm 1914. Trong quá trình tồn tại, chợ từng bị hư hại trong chiến tranh và trải qua nhiều đợt trùng tu, sửa chữa. Sau năm 1975, Chợ Bến Thành tiếp tục giữ vai trò là một trung tâm thương nghiệp quan trọng của thành phố.
```

#### Chunk 3

```text
## HIGHLIGHTS
Kiến trúc Chợ Bến Thành nổi bật với tháp đồng hồ ở mặt tiền Cửa Nam. Tòa tháp nhô cao khỏi mái và có đồng hồ tròn ở các mặt, tạo nên dấu hiệu nhận diện quen thuộc của công trình. Bốn khối cổng chính còn có các phù điêu gốm mô tả nhiều loại sản vật, phản ánh những nhóm hàng từng được bày bán theo từng khu vực. Kết cấu nhà chợ sử dụng gạch chịu lực, khung sắt, cột kèo bê tông và mái ngói.
```

#### Chunk 4

```text
## EXPERIENCE
Trải nghiệm tại Chợ Bến Thành gắn với việc đi qua các khu bán hàng, quan sát nhịp sống chợ truyền thống và mua sắm nhiều nhóm sản phẩm. Các nguồn du lịch của thành phố giới thiệu chợ với các mặt hàng như quần áo, giày dép, vải sợi, hàng truyền thống, đồ gốm, quà lưu niệm và món ăn. Vì vậy, đây là nơi phù hợp để tìm hiểu không khí thương mại địa phương thông qua hoạt động tham quan và mua sắm trực tiếp.
```

---

### Configuration B — Recursive 800/100

Statistics: 4 chunks; min 397; average 408.5; max 419 characters.

#### Chunk 1

```text
# cho-ben-thanh

## OVERVIEW
Chợ Bến Thành là một chợ truyền thống nằm ở khu vực trung tâm Thành phố Hồ Chí Minh và là một địa điểm quen thuộc trong hoạt động tham quan, mua sắm của du khách. Công trình chợ hiện nay có lịch sử hơn một thế kỷ và vẫn duy trì vai trò thương mại. Không gian chợ gắn với hình ảnh đô thị trung tâm, hoạt động buôn bán đa dạng và trải nghiệm văn hóa đời sống địa phương.
```

#### Chunk 2

```text
## BACKGROUND
Khu chợ hiện nay được hình thành từ chủ trương xây dựng một chợ trung tâm vào đầu thế kỷ XX. Công trình nhà lồng được khởi công trong giai đoạn 1913-1914 và lễ khai thị được tổ chức năm 1914. Trong quá trình tồn tại, chợ từng bị hư hại trong chiến tranh và trải qua nhiều đợt trùng tu, sửa chữa. Sau năm 1975, Chợ Bến Thành tiếp tục giữ vai trò là một trung tâm thương nghiệp quan trọng của thành phố.
```

#### Chunk 3

```text
## HIGHLIGHTS
Kiến trúc Chợ Bến Thành nổi bật với tháp đồng hồ ở mặt tiền Cửa Nam. Tòa tháp nhô cao khỏi mái và có đồng hồ tròn ở các mặt, tạo nên dấu hiệu nhận diện quen thuộc của công trình. Bốn khối cổng chính còn có các phù điêu gốm mô tả nhiều loại sản vật, phản ánh những nhóm hàng từng được bày bán theo từng khu vực. Kết cấu nhà chợ sử dụng gạch chịu lực, khung sắt, cột kèo bê tông và mái ngói.
```

#### Chunk 4

```text
## EXPERIENCE
Trải nghiệm tại Chợ Bến Thành gắn với việc đi qua các khu bán hàng, quan sát nhịp sống chợ truyền thống và mua sắm nhiều nhóm sản phẩm. Các nguồn du lịch của thành phố giới thiệu chợ với các mặt hàng như quần áo, giày dép, vải sợi, hàng truyền thống, đồ gốm, quà lưu niệm và món ăn. Vì vậy, đây là nơi phù hợp để tìm hiểu không khí thương mại địa phương thông qua hoạt động tham quan và mua sắm trực tiếp.
```

---

### Configuration C — MarkdownHeader + Recursive 800/100

Statistics: 4 chunks; min 368; average 390.8; max 405 characters.

#### Chunk 1

Section: `OVERVIEW`

```text
Chợ Bến Thành là một chợ truyền thống nằm ở khu vực trung tâm Thành phố Hồ Chí Minh và là một địa điểm quen thuộc trong hoạt động tham quan, mua sắm của du khách. Công trình chợ hiện nay có lịch sử hơn một thế kỷ và vẫn duy trì vai trò thương mại. Không gian chợ gắn với hình ảnh đô thị trung tâm, hoạt động buôn bán đa dạng và trải nghiệm văn hóa đời sống địa phương.
```

#### Chunk 2

Section: `BACKGROUND`

```text
Khu chợ hiện nay được hình thành từ chủ trương xây dựng một chợ trung tâm vào đầu thế kỷ XX. Công trình nhà lồng được khởi công trong giai đoạn 1913-1914 và lễ khai thị được tổ chức năm 1914. Trong quá trình tồn tại, chợ từng bị hư hại trong chiến tranh và trải qua nhiều đợt trùng tu, sửa chữa. Sau năm 1975, Chợ Bến Thành tiếp tục giữ vai trò là một trung tâm thương nghiệp quan trọng của thành phố.
```

#### Chunk 3

Section: `HIGHLIGHTS`

```text
Kiến trúc Chợ Bến Thành nổi bật với tháp đồng hồ ở mặt tiền Cửa Nam. Tòa tháp nhô cao khỏi mái và có đồng hồ tròn ở các mặt, tạo nên dấu hiệu nhận diện quen thuộc của công trình. Bốn khối cổng chính còn có các phù điêu gốm mô tả nhiều loại sản vật, phản ánh những nhóm hàng từng được bày bán theo từng khu vực. Kết cấu nhà chợ sử dụng gạch chịu lực, khung sắt, cột kèo bê tông và mái ngói.
```

#### Chunk 4

Section: `EXPERIENCE`

```text
Trải nghiệm tại Chợ Bến Thành gắn với việc đi qua các khu bán hàng, quan sát nhịp sống chợ truyền thống và mua sắm nhiều nhóm sản phẩm. Các nguồn du lịch của thành phố giới thiệu chợ với các mặt hàng như quần áo, giày dép, vải sợi, hàng truyền thống, đồ gốm, quà lưu niệm và món ăn. Vì vậy, đây là nơi phù hợp để tìm hiểu không khí thương mại địa phương thông qua hoạt động tham quan và mua sắm trực tiếp.
```

---

## Place: thao-cam-vien-sai-gon

### Configuration A — Recursive 500/50

Statistics: 4 chunks; min 404; average 439.0; max 472 characters.

#### Chunk 1

```text
# thao-cam-vien-sai-gon

## OVERVIEW
Thảo Cầm Viên Sài Gòn là một vườn thú và vườn thực vật lâu đời tại Thành phố Hồ Chí Minh, được thành lập từ năm 1864. Không gian này kết hợp chức năng tham quan, học tập về sinh thái, bảo tồn thiên nhiên và vui chơi giải trí. Qua thời gian, Thảo Cầm Viên trở thành một thiết chế văn hóa gắn với ký ức đô thị, đồng thời duy trì các hoạt động chăm sóc, trưng bày và bảo tồn nhiều loài động thực vật.
```

#### Chunk 2

```text
## BACKGROUND
Thảo Cầm Viên Sài Gòn bắt đầu từ một khu vườn ươm và khu nuôi động vật được hình thành trong thập niên 1860. Sau khi khu vườn bách thảo cơ bản được hoàn thành, hoạt động sưu tập và ươm trồng cây cùng với xây dựng chuồng nuôi chim thú tiếp tục phát triển. Năm 1956, khu vườn được đại tu và tên gọi Thảo Cầm Viên Sài Gòn được sử dụng chính thức. Các giai đoạn sau tiếp tục mở rộng chuồng trại, hoạt động bảo tồn và hợp tác chuyên môn.
```

#### Chunk 3

```text
## HIGHLIGHTS
Điểm nổi bật của Thảo Cầm Viên là sự kết hợp giữa hệ động vật, hệ thực vật, không gian xanh lâu năm và hoạt động bảo tồn đa dạng sinh học. Đơn vị công bố đang chăm sóc và trưng bày nhiều loài thực vật và động vật, trong đó có các loài quý hiếm. Các cây cổ thụ, công trình cũ, vườn thú và cảnh quan xanh cùng tồn tại trong một không gian, tạo nên giá trị vừa sinh thái vừa văn hóa - lịch sử.
```

#### Chunk 4

```text
## EXPERIENCE
Khách tham quan có thể quan sát động vật, tìm hiểu thực vật và tiếp cận các nội dung giáo dục về đa dạng sinh học. Thảo Cầm Viên còn phát triển các chương trình học tập ngoài nhà trường, trải nghiệm khoa học và hoạt động ngoại khóa, vì vậy nơi đây không chỉ phục vụ tham quan mà còn có vai trò như một không gian học tập về thiên nhiên. Một số khu trưng bày, trong đó có bảo tàng động thực vật, bổ sung thêm trải nghiệm tìm hiểu mẫu vật và cấu trúc sinh học.
```

---

### Configuration B — Recursive 800/100

Statistics: 4 chunks; min 404; average 439.0; max 472 characters.

#### Chunk 1

```text
# thao-cam-vien-sai-gon

## OVERVIEW
Thảo Cầm Viên Sài Gòn là một vườn thú và vườn thực vật lâu đời tại Thành phố Hồ Chí Minh, được thành lập từ năm 1864. Không gian này kết hợp chức năng tham quan, học tập về sinh thái, bảo tồn thiên nhiên và vui chơi giải trí. Qua thời gian, Thảo Cầm Viên trở thành một thiết chế văn hóa gắn với ký ức đô thị, đồng thời duy trì các hoạt động chăm sóc, trưng bày và bảo tồn nhiều loài động thực vật.
```

#### Chunk 2

```text
## BACKGROUND
Thảo Cầm Viên Sài Gòn bắt đầu từ một khu vườn ươm và khu nuôi động vật được hình thành trong thập niên 1860. Sau khi khu vườn bách thảo cơ bản được hoàn thành, hoạt động sưu tập và ươm trồng cây cùng với xây dựng chuồng nuôi chim thú tiếp tục phát triển. Năm 1956, khu vườn được đại tu và tên gọi Thảo Cầm Viên Sài Gòn được sử dụng chính thức. Các giai đoạn sau tiếp tục mở rộng chuồng trại, hoạt động bảo tồn và hợp tác chuyên môn.
```

#### Chunk 3

```text
## HIGHLIGHTS
Điểm nổi bật của Thảo Cầm Viên là sự kết hợp giữa hệ động vật, hệ thực vật, không gian xanh lâu năm và hoạt động bảo tồn đa dạng sinh học. Đơn vị công bố đang chăm sóc và trưng bày nhiều loài thực vật và động vật, trong đó có các loài quý hiếm. Các cây cổ thụ, công trình cũ, vườn thú và cảnh quan xanh cùng tồn tại trong một không gian, tạo nên giá trị vừa sinh thái vừa văn hóa - lịch sử.
```

#### Chunk 4

```text
## EXPERIENCE
Khách tham quan có thể quan sát động vật, tìm hiểu thực vật và tiếp cận các nội dung giáo dục về đa dạng sinh học. Thảo Cầm Viên còn phát triển các chương trình học tập ngoài nhà trường, trải nghiệm khoa học và hoạt động ngoại khóa, vì vậy nơi đây không chỉ phục vụ tham quan mà còn có vai trò như một không gian học tập về thiên nhiên. Một số khu trưng bày, trong đó có bảo tàng động thực vật, bổ sung thêm trải nghiệm tìm hiểu mẫu vật và cấu trúc sinh học.
```

---

### Configuration C — MarkdownHeader + Recursive 800/100

Statistics: 4 chunks; min 390; average 419.2; max 458 characters.

#### Chunk 1

Section: `OVERVIEW`

```text
Thảo Cầm Viên Sài Gòn là một vườn thú và vườn thực vật lâu đời tại Thành phố Hồ Chí Minh, được thành lập từ năm 1864. Không gian này kết hợp chức năng tham quan, học tập về sinh thái, bảo tồn thiên nhiên và vui chơi giải trí. Qua thời gian, Thảo Cầm Viên trở thành một thiết chế văn hóa gắn với ký ức đô thị, đồng thời duy trì các hoạt động chăm sóc, trưng bày và bảo tồn nhiều loài động thực vật.
```

#### Chunk 2

Section: `BACKGROUND`

```text
Thảo Cầm Viên Sài Gòn bắt đầu từ một khu vườn ươm và khu nuôi động vật được hình thành trong thập niên 1860. Sau khi khu vườn bách thảo cơ bản được hoàn thành, hoạt động sưu tập và ươm trồng cây cùng với xây dựng chuồng nuôi chim thú tiếp tục phát triển. Năm 1956, khu vườn được đại tu và tên gọi Thảo Cầm Viên Sài Gòn được sử dụng chính thức. Các giai đoạn sau tiếp tục mở rộng chuồng trại, hoạt động bảo tồn và hợp tác chuyên môn.
```

#### Chunk 3

Section: `HIGHLIGHTS`

```text
Điểm nổi bật của Thảo Cầm Viên là sự kết hợp giữa hệ động vật, hệ thực vật, không gian xanh lâu năm và hoạt động bảo tồn đa dạng sinh học. Đơn vị công bố đang chăm sóc và trưng bày nhiều loài thực vật và động vật, trong đó có các loài quý hiếm. Các cây cổ thụ, công trình cũ, vườn thú và cảnh quan xanh cùng tồn tại trong một không gian, tạo nên giá trị vừa sinh thái vừa văn hóa - lịch sử.
```

#### Chunk 4

Section: `EXPERIENCE`

```text
Khách tham quan có thể quan sát động vật, tìm hiểu thực vật và tiếp cận các nội dung giáo dục về đa dạng sinh học. Thảo Cầm Viên còn phát triển các chương trình học tập ngoài nhà trường, trải nghiệm khoa học và hoạt động ngoại khóa, vì vậy nơi đây không chỉ phục vụ tham quan mà còn có vai trò như một không gian học tập về thiên nhiên. Một số khu trưng bày, trong đó có bảo tàng động thực vật, bổ sung thêm trải nghiệm tìm hiểu mẫu vật và cấu trúc sinh học.
```

---

## Place: chua-ba-thien-hau

### Configuration A — Recursive 500/50

Statistics: 4 chunks; min 312; average 362.8; max 424 characters.

#### Chunk 1

```text
# chua-ba-thien-hau

## OVERVIEW
Chùa Bà Thiên Hậu là một cơ sở tín ngưỡng lâu đời của cộng đồng người Hoa tại khu Chợ Lớn, Thành phố Hồ Chí Minh, thờ Thiên Hậu Thánh Mẫu. Địa điểm mang giá trị tôn giáo, lịch sử và văn hóa cộng đồng, phù hợp với người muốn khám phá không gian tín ngưỡng và dấu ấn văn hóa người Hoa trong đô thị Sài Gòn - Chợ Lớn.
```

#### Chunk 2

```text
## BACKGROUND
Theo Cục Du lịch Quốc gia Việt Nam, ngôi chùa được cộng đồng người Hoa xây dựng vào khoảng năm 1760 sau khi họ định cư tại vùng Chợ Lớn. Việc thờ Thiên Hậu Thánh Mẫu gắn với niềm tin cầu mong sự che chở trong hành trình vượt biển và cuộc sống tại vùng đất mới, qua đó phản ánh lịch sử di cư và sinh hoạt cộng đồng của người Hoa tại Thành phố Hồ Chí Minh.
```

#### Chunk 3

```text
## HIGHLIGHTS
Không gian chùa nổi bật với kiến trúc và trang trí mang dấu ấn văn hóa Hoa, cùng những vòng nhang lớn treo trong khu vực sân tạo nên hình ảnh đặc trưng. Bên cạnh vai trò là nơi thờ tự, chùa còn gắn với các sinh hoạt lễ hội của cộng đồng, đặc biệt là lễ vía Bà Thiên Hậu vào ngày 23 tháng 3 âm lịch.
```

#### Chunk 4

```text
## EXPERIENCE
Khách tham quan có thể quan sát kiến trúc, các chi tiết trang trí và không gian thờ tự, đồng thời cảm nhận nhịp sống văn hóa của cộng đồng người Hoa tại Chợ Lớn. Vì đây là địa điểm tín ngưỡng đang hoạt động, trải nghiệm phù hợp nhất là tham quan với trang phục lịch sự, giữ yên tĩnh và tôn trọng người hành lễ; địa điểm cũng dễ kết hợp với Chợ Bình Tây và các hội quán lân cận trong một tuyến khám phá Chợ Lớn.
```

---

### Configuration B — Recursive 800/100

Statistics: 2 chunks; min 717; average 727.5; max 738 characters.

#### Chunk 1

```text
# chua-ba-thien-hau

## OVERVIEW
Chùa Bà Thiên Hậu là một cơ sở tín ngưỡng lâu đời của cộng đồng người Hoa tại khu Chợ Lớn, Thành phố Hồ Chí Minh, thờ Thiên Hậu Thánh Mẫu. Địa điểm mang giá trị tôn giáo, lịch sử và văn hóa cộng đồng, phù hợp với người muốn khám phá không gian tín ngưỡng và dấu ấn văn hóa người Hoa trong đô thị Sài Gòn - Chợ Lớn.

## BACKGROUND
Theo Cục Du lịch Quốc gia Việt Nam, ngôi chùa được cộng đồng người Hoa xây dựng vào khoảng năm 1760 sau khi họ định cư tại vùng Chợ Lớn. Việc thờ Thiên Hậu Thánh Mẫu gắn với niềm tin cầu mong sự che chở trong hành trình vượt biển và cuộc sống tại vùng đất mới, qua đó phản ánh lịch sử di cư và sinh hoạt cộng đồng của người Hoa tại Thành phố Hồ Chí Minh.
```

#### Chunk 2

```text
## HIGHLIGHTS
Không gian chùa nổi bật với kiến trúc và trang trí mang dấu ấn văn hóa Hoa, cùng những vòng nhang lớn treo trong khu vực sân tạo nên hình ảnh đặc trưng. Bên cạnh vai trò là nơi thờ tự, chùa còn gắn với các sinh hoạt lễ hội của cộng đồng, đặc biệt là lễ vía Bà Thiên Hậu vào ngày 23 tháng 3 âm lịch.

## EXPERIENCE
Khách tham quan có thể quan sát kiến trúc, các chi tiết trang trí và không gian thờ tự, đồng thời cảm nhận nhịp sống văn hóa của cộng đồng người Hoa tại Chợ Lớn. Vì đây là địa điểm tín ngưỡng đang hoạt động, trải nghiệm phù hợp nhất là tham quan với trang phục lịch sự, giữ yên tĩnh và tôn trọng người hành lễ; địa điểm cũng dễ kết hợp với Chợ Bình Tây và các hội quán lân cận trong một tuyến khám phá Chợ Lớn.
```

---

### Configuration C — MarkdownHeader + Recursive 800/100

Statistics: 4 chunks; min 298; average 344.0; max 410 characters.

#### Chunk 1

Section: `OVERVIEW`

```text
Chùa Bà Thiên Hậu là một cơ sở tín ngưỡng lâu đời của cộng đồng người Hoa tại khu Chợ Lớn, Thành phố Hồ Chí Minh, thờ Thiên Hậu Thánh Mẫu. Địa điểm mang giá trị tôn giáo, lịch sử và văn hóa cộng đồng, phù hợp với người muốn khám phá không gian tín ngưỡng và dấu ấn văn hóa người Hoa trong đô thị Sài Gòn - Chợ Lớn.
```

#### Chunk 2

Section: `BACKGROUND`

```text
Theo Cục Du lịch Quốc gia Việt Nam, ngôi chùa được cộng đồng người Hoa xây dựng vào khoảng năm 1760 sau khi họ định cư tại vùng Chợ Lớn. Việc thờ Thiên Hậu Thánh Mẫu gắn với niềm tin cầu mong sự che chở trong hành trình vượt biển và cuộc sống tại vùng đất mới, qua đó phản ánh lịch sử di cư và sinh hoạt cộng đồng của người Hoa tại Thành phố Hồ Chí Minh.
```

#### Chunk 3

Section: `HIGHLIGHTS`

```text
Không gian chùa nổi bật với kiến trúc và trang trí mang dấu ấn văn hóa Hoa, cùng những vòng nhang lớn treo trong khu vực sân tạo nên hình ảnh đặc trưng. Bên cạnh vai trò là nơi thờ tự, chùa còn gắn với các sinh hoạt lễ hội của cộng đồng, đặc biệt là lễ vía Bà Thiên Hậu vào ngày 23 tháng 3 âm lịch.
```

#### Chunk 4

Section: `EXPERIENCE`

```text
Khách tham quan có thể quan sát kiến trúc, các chi tiết trang trí và không gian thờ tự, đồng thời cảm nhận nhịp sống văn hóa của cộng đồng người Hoa tại Chợ Lớn. Vì đây là địa điểm tín ngưỡng đang hoạt động, trải nghiệm phù hợp nhất là tham quan với trang phục lịch sự, giữ yên tĩnh và tôn trọng người hành lễ; địa điểm cũng dễ kết hợp với Chợ Bình Tây và các hội quán lân cận trong một tuyến khám phá Chợ Lớn.
```

---

## Final Summary

| Configuration | Total chunks | Avg chunk length |
|---|---:|---:|
| Recursive 500/50 | 24 | 393.5 |
| Recursive 800/100 | 19 | 497.5 |
| MarkdownHeader + Recursive fallback | 24 | 374.3 |
