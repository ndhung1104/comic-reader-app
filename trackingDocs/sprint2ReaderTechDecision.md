# Sprint 2 Reader - Technical Decision Record

- Date: 18/03/2026
- Owner: Hung (Reader)
- Scope sprint: 16/03/2026 - 30/03/2026
- Related docs:
  - `trackingDocs/sprint2ReaderTechDocs.md`
  - `trackingDocs/sprintTimeline.md` (Sprint 2 / Hung)
  - `trackingDocs/listFeatures.md` (muc 9, muc 10)

## 1) Muc tieu

Nang cap Reader de dat 3 muc tieu sprint:
- Pinch-to-zoom on dinh, khong vo layout nghiem trong.
- Cuon muot chapter dai (lazy loading + preload).
- Luu va khoi phuc vi tri doc gan nhat.

Rang buoc quan trong:
- Khong lam hong flow free/premium unlock chapter hien tai.
- Out of scope: auto-scroll hoan chinh, reader ngang hoan chinh.

## 2) Hien trang ky thuat (dua tren code)

- Reader hien tai: `RecyclerView + Glide`, load danh sach anh mot lan.
  - `mobile/app/src/main/java/com/group09/ComicReader/ui/reader/ReaderActivity.java`
  - `mobile/app/src/main/java/com/group09/ComicReader/adapter/ReaderPageAdapter.java`
- Adapter dang dung `notifyDataSetChanged()` => nguy co jank khi chapter dai.
- Chua co zoom, chua preload, chua luu vi tri doc.
- SharedPreferences da co cho session auth:
  - `mobile/app/src/main/java/com/group09/ComicReader/data/local/SessionManager.java`
- Flow khoa/mo chapter dang duoc chan truoc khi vao Reader:
  - `mobile/app/src/main/java/com/group09/ComicReader/ui/comic/ComicDetailFragment.java`

## 3) Danh gia va cham diem

Thang diem:
- Chat luong giai phap: 0-10 (10 la tot nhat).
- Do phuc tap trien khai: 1-10 (10 la phuc tap/rui ro cao nhat).

| Hang muc | Hien tai | Trong sprint2ReaderTechDocs | Do phuc tap neu ap dung docs | Nhan xet |
|---|---:|---:|---:|---|
| Zoom anh | 3.0 | 8.0 | 8 | Huong SSIV dung cho anh cuc dai, nhung can rollout theo pha de tranh qua tai sprint. |
| Lazy loading + preload | 4.0 | 8.5 | 6 | DiffUtil/AsyncListDiffer + preload la lua chon rat hop ly cho Sprint 2. |
| Luu vi tri doc | 2.5 | 8.0 | 7 | DataStore + debounce la huong dung dai han; lam full migration ngay co rui ro tien do. |
| Tuong thich Android 15/16 | 6.5 | 6.0 | 4 | Dung xu huong, nhung uu tien sau vi du an hien tai chua thay NDK/native stack. |
| Do phu hop deadline Sprint 2 | 5.5 | 6.0 | 7 | Neu lam full theo docs se de tre deadline va tang regression risk. |

Tong hop diem:
- Muc san sang hien tai: **4.3/10**
- Chat luong huong de xuat trong docs: **7.3/10**
- Muc phu hop deadline sprint cua docs: **6.1/10**

## 4) Quyet dinh cong nghe cho Sprint 2

### 4.1 Zoom

Quyet dinh:
- Sprint 2 ap dung giai phap zoom nhe, on dinh cho da so chapter.
- Chua bat buoc chuyen toan bo sang SSIV ngay lap tuc.
- Them tieu chi kich hoat SSIV (pha 2) neu gap chapter co anh sieu dai/sieu nang.

Ly do:
- Giam rui ro regression va giu dung timeline.
- Dam bao AC "zoom on dinh, khong vo layout".

### 4.2 Lazy loading + preload

Quyet dinh:
- Refactor `ReaderPageAdapter` sang `ListAdapter` + `DiffUtil`.
- Preload anh trang tiep theo bang Glide preload target.
- Tinh chinh `RecyclerView` (cache/prefetch) cho chapter dai.

Ly do:
- Tang FPS va giam jank ro ret nhat trong sprint.
- Effort vua phai, de verify bang test manual.

### 4.3 Luu/khoi phuc vi tri doc

Quyet dinh:
- Sprint 2: implement progress store co debounce ghi dia.
- Sprint 2 co the dung SharedPreferences co throttle/debounce dung cach.
- DataStore de dua vao Sprint 3 (hardening), neu con buffer thi pilot truoc 1 key.

Ly do:
- Dat AC kinh doanh ngay trong sprint.
- Giam nguy co vo tien do do migration lon.

### 4.4 Bao toan flow free/premium

Quyet dinh:
- Khong thay doi logic unlock.
- Restore vi tri doc phai xac nhan chapter con unlocked; neu khong thi fallback chapter hop le.

Ly do:
- Dap ung acceptance "khong lam hong luong free/premium hien tai".

## 5) Ke hoach thuc thi va effort

Tong effort uoc tinh: **9-11 dev-days** (1 dev).

| Work item | Effort | Do phuc tap | Ownership | Ket qua |
|---|---:|---:|---|---|
| Reader adapter perf (ListAdapter + DiffUtil) | 1.5-2.0 ngay | 5/10 | Hung | Giam update thua, cuon muot hon |
| Glide preload + RecyclerView tuning | 1.5-2.0 ngay | 6/10 | Hung | Giam lag chapter dai |
| Zoom implementation (pha 1) | 2.0-2.5 ngay | 7/10 | Hung | Pinch-to-zoom on dinh |
| Save/restore reading position | 2.0-2.5 ngay | 7/10 | Hung | Mo lai truy cap dung vi tri gan nhat |
| Regression test free/premium + long chapter | 1.5-2.0 ngay | 5/10 | Hung | Khong vo flow hien tai |

## 6) Rui ro va giam thieu

1. Rui ro jank khi zoom + nested gesture voi RecyclerView.
- Giam thieu: khoanh vung gesture, khoa fling khi dang pinch, test tren chapter dai.

2. Rui ro ghi vi tri doc qua nhieu gay drop frame.
- Giam thieu: debounce 1-2s + ghi dinh ky (khong ghi moi pixel).

3. Rui ro regression free/premium.
- Giam thieu: giu nguyen gate unlock; them test case locked chapter, unlocked chapter, resume.

4. Rui ro qua tai scope neu dua SSIV + DataStore full migration ngay.
- Giam thieu: rollout theo pha, uu tien AC sprint.

## 7) KPI/Acceptance mapping

- AC1 Zoom on dinh: pass khi pinch/zoom lien tuc 5 phut khong crash, khong vo layout nghiem trong.
- AC2 Reader muot chapter dai: pass khi chapter dai giam hien tuong khung/khung hinh rot ro ret.
- AC3 Resume vi tri doc: pass khi thoat/mo lai quay ve chapter + vi tri gan nhat.
- AC4 Khong hong free/premium: pass khi chapter lock van lock, chapter unlock van doc binh thuong.

## 8) Ket luan

Phuong an toi uu cho Sprint 2 la **hybrid-pragmatic**:
- Lay cac de xuat dung va co ROI cao ngay: DiffUtil, preload, debounce progress.
- Trien khai zoom theo pha de dat deadline va giam regression.
- DataStore/SSIV full migration dua vao pha tiep theo neu metric su dung cho thay can thiet.

Target sau sprint: nang diem san sang Reader tu **4.3/10** len **>= 8.0/10** cho scope Sprint 2.
