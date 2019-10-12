package hello.status;

public enum SexStatus {
    /**
     有三種狀態

     1. 沒有元素
     SEX_STATUS_ISNOTEXISTS
     直接加載

     2. 有元素超時
     SEX_STATUS_TIMEOUT
     返回一組後加載

     3. 有元素不超時
     SEX_STATUS_SUCCESS
     直接返回
     */
    SEX_STATUS_SUCCESS,SEX_STATUS_ISNOTEXISTS,SEX_STATUS_TIMEOUT;
}
