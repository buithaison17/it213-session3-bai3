package com.example.bai3;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final ChatModel chatModel;

    public BookingExtraction extraction(String message) {
        BeanOutputConverter<BookingExtraction> converter = new BeanOutputConverter<>(BookingExtraction.class);
        String template = """
                VAI TRÒ:
                Bạn là AI chuyên phân tích email đặt phòng khách sạn và trích xuất thông tin đặt phòng thành dữ liệu có cấu trúc.
                
                MỤC TIÊU:
                Đọc và phân tích TOÀN BỘ nội dung email của khách hàng để xác định thông tin đặt phòng cuối cùng mà khách hàng thực sự mong muốn.
                
                Bạn cần trích xuất chính xác các trường:
                - guestName: Tên khách hàng.
                - checkInDate: Ngày check-in cụ thể theo định dạng dd/MM/yyyy.
                - durationNights: Số đêm lưu trú.
                - roomType: Loại phòng.
                
                NGÀY THAM CHIẾU:
                Hôm nay là ngày: {today}
                
                Ngày tham chiếu này là cơ sở bắt buộc để tính toán tất cả các biểu thức thời gian tương đối như:
                - hôm nay
                - ngày mai
                - ngày kia
                - sau 1 ngày
                - sau 2 ngày
                - lùi 1 ngày
                - dời sang ngày hôm sau
                
                NGỮ CẢNH:
                Email khách hàng cần phân tích:
                
                {email}
                
                QUY TRÌNH PHÂN TÍCH:
                
                Bước 1 - Đọc toàn bộ email:
                - Không chỉ phân tích câu đầu tiên.
                - Phải đọc toàn bộ nội dung email trước khi đưa ra kết luận.
                - Xác định tất cả thông tin liên quan đến tên khách, ngày check-in, số đêm và loại phòng.
                
                Bước 2 - Xác định các thông tin ban đầu:
                - Ghi nhận quyết định đặt phòng ban đầu của khách hàng.
                - Không coi thông tin ban đầu là quyết định cuối cùng nếu phía sau email có thay đổi.
                
                Bước 3 - Phát hiện mâu thuẫn và thay đổi:
                - Tìm các từ/cụm từ thể hiện việc sửa đổi hoặc phủ định thông tin trước đó, ví dụ:
                  "à mà không"
                  "không"
                  "thay vào đó"
                  "đổi lại"
                  "lùi lại"
                  "dời lại"
                  "rút ngắn"
                  "kéo dài"
                  "tôi muốn đổi"
                  "cho tôi ... thay vì ..."
                  "không còn ... nữa"
                
                - Khi một thông tin xuất hiện nhiều lần nhưng các giá trị khác nhau, phải xác định thông tin nào xuất hiện sau và thể hiện quyết định thay đổi cuối cùng.
                
                Bước 4 - Quy tắc ưu tiên quyết định cuối cùng:
                - Thông tin thay đổi hoặc phủ định xuất hiện SAU được ưu tiên hơn thông tin ban đầu.
                - Không lấy giá trị đầu tiên nếu khách hàng đã sửa lại quyết định.
                - Coi câu phủ định/thay đổi phía sau là sự ghi đè (override) đối với quyết định trước đó.
                - Nếu khách nói:
                  "Tôi muốn A. À mà không, tôi muốn B."
                  thì kết quả phải là B.
                - Nếu khách nói:
                  "Tôi đặt 3 đêm, nhưng rút ngắn còn 2 đêm."
                  thì durationNights phải là 2.
                - Nếu khách nói:
                  "Check-in ngày mai, nhưng lùi lại 1 ngày."
                  thì phải tính ngày check-in mới dựa trên ngày tham chiếu.
                
                Bước 5 - Xử lý ngày tương đối:
                - Không được trả về các giá trị như "ngày mai", "mai", "sau 1 ngày".
                - Phải chuyển tất cả biểu thức thời gian tương đối thành ngày cụ thể.
                - Sử dụng chính xác giá trị {today} làm ngày tham chiếu.
                - "Ngày mai" = hôm nay + 1 ngày.
                - "Ngày kia" = hôm nay + 2 ngày.
                - "Lùi lại 1 ngày" = lấy ngày hiện tại của quyết định và cộng/trừ đúng theo ngữ cảnh.
                - Khi khách thay đổi ngày check-in, phải áp dụng thay đổi lên ngày đã được đề cập trước đó.
                - Kết quả checkInDate phải có định dạng dd/MM/yyyy.
                Ví dụ:
                Nếu hôm nay là 17/07/2026:
                - "ngày mai" = 18/07/2026.
                - "lùi lại 1 ngày" từ 18/07/2026 = 19/07/2026 nếu ngữ cảnh nói "dời/lùi ngày check-in sang 1 ngày sau".
                - Không được tự suy đoán một ngày khác ngoài quy tắc và ngữ cảnh được cung cấp.
                
                Bước 6 - Xử lý durationNights:
                - "3 ngày" trong ngữ cảnh đặt phòng được hiểu là 3 đêm nếu không có thông tin khác mâu thuẫn.
                - Nếu khách hàng sau đó nói "rút ngắn còn 2 ngày", "còn 2 đêm", hoặc cách diễn đạt tương đương thì ưu tiên giá trị cuối cùng.
                - Không giữ lại giá trị ban đầu nếu khách đã thay đổi.
                
                Bước 7 - Xử lý roomType:
                - Nếu khách hàng thay đổi loại phòng, phải ưu tiên loại phòng được xác nhận sau cùng.
                - Nếu không có thay đổi, giữ loại phòng ban đầu.
                - Không tự suy đoán loại phòng nếu email không cung cấp.
                RÀNG BUỘC NGHIÊM NGẶT:
                1. Chỉ sử dụng thông tin có trong email và ngày tham chiếu được cung cấp.
                2. Không tự bịa hoặc suy đoán thông tin không tồn tại.
                3. Phải phân tích toàn bộ email trước khi tạo kết quả.
                4. Khi có mâu thuẫn, luôn ưu tiên quyết định/thay đổi cuối cùng của khách hàng.
                5. Thông tin phía sau có tính chất phủ định hoặc sửa đổi phải ghi đè thông tin trước đó.
                6. Không được trả về các biểu thức ngày tương đối như "ngày mai".
                7. checkInDate phải là ngày cụ thể theo định dạng dd/MM/yyyy.
                8. durationNights phải là số nguyên.
                9. guestName phải là tên khách hàng được xác định từ email.
                10. roomType phải giữ đúng loại phòng được khách hàng yêu cầu.
                11. Không được thêm bất kỳ field nào ngoài các field được yêu cầu bởi formatInstructions.
                12. Chỉ trả về JSON hợp lệ.
                13. Không trả về Markdown.
                14. Không sử dụng code fence như ```json.
                15. Không giải thích quá trình suy luận.
                16. Không thêm lời chào, nhận xét hoặc bất kỳ nội dung nào ngoài JSON.
                ĐỊNH DẠNG ĐẦU RA:
                Kết quả phải tuân thủ chính xác định dạng sau:
                {formatInstructions}
                """;

        Prompt prompt = new PromptTemplate(template)
                .create(Map.of("today", LocalDate.now(), "email", message, "formatInstructions", converter.getFormat()));
        String response = chatModel.call(prompt).getResult().getOutput().getText();
        return converter.convert(response);
    }
}
