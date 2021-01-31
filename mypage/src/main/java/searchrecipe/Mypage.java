package searchrecipe;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="Mypage_table")
public class Mypage {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private Long orderId;
        private Long deliveryId;
        private String status;
        private String materialNm;
        private Integer qty;
        private Long recipeId;
        private String recipeNm;
        private String cookingMethod;
        private String materialList;
        private String materialId;


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }
        public Long getDeliveryId() {
            return deliveryId;
        }

        public void setDeliveryId(Long deliveryId) {
            this.deliveryId = deliveryId;
        }
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
        public String getMaterialNm() {
            return materialNm;
        }

        public void setMaterialNm(String materialNm) {
            this.materialNm = materialNm;
        }
        public Integer getQty() {
            return qty;
        }

        public void setQty(Integer qty) {
            this.qty = qty;
        }
        public Long getRecipeId() {
            return recipeId;
        }

        public void setRecipeId(Long recipeId) {
            this.recipeId = recipeId;
        }
        public String getRecipeNm() {
            return recipeNm;
        }

        public void setRecipeNm(String recipeNm) {
            this.recipeNm = recipeNm;
        }
        public String getCookingMethod() {
            return cookingMethod;
        }

        public void setCookingMethod(String cookingMethod) {
            this.cookingMethod = cookingMethod;
        }
        public String getMaterialList() {
            return materialList;
        }

        public void setMaterialList(String materialList) {
            this.materialList = materialList;
        }
        public String getMaterialId() {
            return materialId;
        }

        public void setMaterialId(String materialId) {
            this.materialId = materialId;
        }

}
