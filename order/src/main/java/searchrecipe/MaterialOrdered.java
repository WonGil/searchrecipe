
package searchrecipe;

public class MaterialOrdered extends AbstractEvent {

    private Long id;
    private String materialNm;
    private Integer qty;

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
}
