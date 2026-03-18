# Sprint 2 Reader - Manual Test Checklist

- Date: 18/03/2026
- Scope: Sprint 2 (16/03/2026 - 30/03/2026)
- Branch: `sprint2/hung-reader-ux`
- Tester: ____________________
- Device/OS: ____________________
- Build: ____________________

## 1) Muc tieu test

- Xac nhan zoom hoat dong on dinh, khong vo layout nghiem trong.
- Xac nhan reader cuon muot hon voi chapter dai (lazy load + preload).
- Xac nhan thoat/mo lai co the quay lai vi tri doc gan nhat.
- Xac nhan khong lam hong flow free/premium hien tai.

## 2) Tien dieu kien

1. Da build va cai ban app tu nhanh `sprint2/hung-reader-ux`.
2. Co backend local/API du lieu chapter de doc.
3. Co it nhat 1 comic co:
- chapter unlocked/free de test doc binh thuong.
- chapter locked/premium de test gate.
4. Nen co chapter dai (nhieu trang) de test hieu nang cuon.

## 3) Cac testcase bat buoc (Acceptance Criteria)

| ID | Test case | Steps | Expected result | Actual | Pass/Fail | Note |
|---|---|---|---|---|---|---|
| AC1-01 | Pinch zoom in/out tren 1 trang | Mo Reader > dung 2 ngon tay zoom in, zoom out lap lai 10-15 lan | Anh zoom muot, khong crash, khong bi vo man hinh nghiem trong |  |  |  |
| AC1-02 | Pan khi dang zoom | Zoom len > keo anh len/xuong/trai/phai | Pan duoc trong vung anh, thao tac hop ly, khong giat manh |  |  |  |
| AC1-03 | Cuon tiep chapter sau khi zoom | Zoom 1 trang > cuon tiep xuong cac trang sau | Reader van cuon duoc, khong bi khoa cuon toan bo |  |  |  |
| AC2-01 | Cuon chapter dai lien tuc | Mo chapter dai > cuon lien tuc tu dau den gan cuoi | Hien tuong khung/khung hinh rot giam ro, khong dung khung hinh lau |  |  |  |
| AC2-02 | Preload trang tiep theo | Cuon cham qua tung trang trong chapter dai | Trang ke tiep hien thi nhanh hon, giam trang thai cho anh |  |  |  |
| AC2-03 | Quay len/xuong nhieu lan | Cuon xuong, cuon len, lap lai 3-5 chu ky | Van dap ung duoc, khong crash/ANR |  |  |  |
| AC3-01 | Resume trong cung chapter | Doc den giua chapter > back/thoat Reader > mo lai dung chapter do | Quay lai gan vi tri vua doc (position + offset gan dung) |  |  |  |
| AC3-02 | Resume sau khi kill app | Doc den giua chapter > dong app > mo lai vao comic va chapter | Van quay lai vi tri doc gan nhat |  |  |  |
| AC3-03 | Resume tu nut Read o Comic Detail | Doc 1 chapter bat ky > quay ve Comic Detail > bam Read | Uu tien chapter dang co progress (neu con unlocked) |  |  |  |
| AC4-01 | Chapter locked khong mo duoc | Vao Comic Detail > bam chapter locked | Hien thong bao chapter locked, khong vao reader |  |  |  |
| AC4-02 | Chapter unlocked mo binh thuong | Vao Comic Detail > bam chapter unlocked | Mo Reader binh thuong |  |  |  |
| AC4-03 | Progress chapter locked (fallback) | Tao progress o chapter X > chapter X bi lock > bam Read | Khong resume vao chapter lock; fallback chapter hop le unlocked |  |  |  |

## 4) Testcase bo sung de giam rui ro

| ID | Test case | Steps | Expected result | Actual | Pass/Fail | Note |
|---|---|---|---|---|---|---|
| RISK-01 | Doi huong man hinh (neu cho phep) | Dang doc > xoay man hinh | Khong crash, khong mat trang thai bat thuong |  |  |  |
| RISK-02 | Mang cham/khong on dinh | Bat network cham > mo reader | App khong crash, hien thong diep loi hop ly neu fail |  |  |  |
| RISK-03 | Comic co it trang | Mo chapter ngan (1-3 trang) | Zoom/cuon/resume van on dinh |  |  |  |
| RISK-04 | Comic co nhieu chapter | Chuyen qua lai nhieu chapter | Khong roi vao sai chapter, progress khong bi nham comic |  |  |  |

## 5) Tieu chi dong Sprint 2 (Reader)

Dat khi:
1. Toan bo testcase AC1-AC4 deu Pass.
2. Khong co bug blocker lien quan crash/ANR Reader.
3. Khong co regression gate free/premium.

Neu con loi:
1. Ghi ro `ID testcase`, buoc tai hien, device, ban build.
2. Phan loai muc do:
- Blocker: crash, ANR, bypass lock premium.
- Major: resume sai qua xa, zoom gay vo layout nghiem trong.
- Minor: UI rung nhe, trai nghiem chua muot nhung van dung duoc.

## 6) Log bug template (copy dung nhanh)

```text
Bug ID:
Testcase ID:
Device/OS:
Build/Branch:
Steps to reproduce:
Actual:
Expected:
Severity (Blocker/Major/Minor):
Attachment (video/screenshot):
```
