package searchrecipe;

public class MaterialOrdered extends AbstractEvent {

    private Long id;
    private String materialNm;
    private Integer qty;
    private Long orderId;
    private String recipeNm;
    private String cookingMethod;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}