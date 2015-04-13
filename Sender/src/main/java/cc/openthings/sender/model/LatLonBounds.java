package cc.openthings.sender.model;

public final class LatLngBounds implements SafeParcelable {
    public static final g CREATOR = new g();
    private final int CK;
    public final LatLng southwest;
    public final LatLng northeast;

    LatLngBounds(int versionCode, LatLng southwest, LatLng northeast) {
        jx.b(southwest, "null southwest");
        jx.b(northeast, "null northeast");
        jx.b(northeast.latitude >= southwest.latitude, "southern latitude exceeds northern latitude (%s > %s)", new Object[]{Double.valueOf(southwest.latitude), Double.valueOf(northeast.latitude)});
        this.CK = versionCode;
        this.southwest = southwest;
        this.northeast = northeast;
    }

    public LatLngBounds(LatLng southwest, LatLng northeast) {
        this(1, southwest, northeast);
    }

    int getVersionCode() {
        return this.CK;
    }

    public void writeToParcel(Parcel out, int flags) {
        if(aa.ob()) {
            h.a(this, out, flags);
        } else {
            g.a(this, out, flags);
        }

    }

    public int describeContents() {
        return 0;
    }

    public static LatLngBounds.Builder builder() {
        return new LatLngBounds.Builder();
    }

    public boolean contains(LatLng point) {
        return this.c(point.latitude) && this.d(point.longitude);
    }

    public LatLngBounds including(LatLng point) {
        double var3 = Math.min(this.southwest.latitude, point.latitude);
        double var5 = Math.max(this.northeast.latitude, point.latitude);
        double var7 = this.northeast.longitude;
        double var9 = this.southwest.longitude;
        double var11 = point.longitude;
        if(!this.d(var11)) {
            if(b(var9, var11) < c(var7, var11)) {
                var9 = var11;
            } else {
                var7 = var11;
            }
        }

        return new LatLngBounds(new LatLng(var3, var9), new LatLng(var5, var7));
    }

    public LatLng getCenter() {
        double var1 = (this.southwest.latitude + this.northeast.latitude) / 2.0D;
        double var3 = this.northeast.longitude;
        double var5 = this.southwest.longitude;
        double var7;
        if(var5 <= var3) {
            var7 = (var3 + var5) / 2.0D;
        } else {
            var7 = (var3 + 360.0D + var5) / 2.0D;
        }

        return new LatLng(var1, var7);
    }

    private static double b(double var0, double var2) {
        return (var0 - var2 + 360.0D) % 360.0D;
    }

    private static double c(double var0, double var2) {
        return (var2 - var0 + 360.0D) % 360.0D;
    }

    private boolean c(double var1) {
        return this.southwest.latitude <= var1 && var1 <= this.northeast.latitude;
    }

    private boolean d(double var1) {
        return this.southwest.longitude <= this.northeast.longitude?this.southwest.longitude <= var1 && var1 <= this.northeast.longitude:this.southwest.longitude <= var1 || var1 <= this.northeast.longitude;
    }

    public int hashCode() {
        return jv.hashCode(new Object[]{this.southwest, this.northeast});
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof LatLngBounds)) {
            return false;
        } else {
            LatLngBounds var2 = (LatLngBounds)o;
            return this.southwest.equals(var2.southwest) && this.northeast.equals(var2.northeast);
        }
    }

    public String toString() {
        return jv.h(this).a("southwest", this.southwest).a("northeast", this.northeast).toString();
    }

    public static final class Builder {
        private double amk = 1.0D / 0.0;
        private double aml = -1.0D / 0.0;
        private double amm = 0.0D / 0.0;
        private double amn = 0.0D / 0.0;

        public Builder() {
        }

        public LatLngBounds.Builder include(LatLng point) {
            this.amk = Math.min(this.amk, point.latitude);
            this.aml = Math.max(this.aml, point.latitude);
            double var2 = point.longitude;
            if(Double.isNaN(this.amm)) {
                this.amm = var2;
                this.amn = var2;
            } else if(!this.d(var2)) {
                if(LatLngBounds.d(this.amm, var2) < LatLngBounds.e(this.amn, var2)) {
                    this.amm = var2;
                } else {
                    this.amn = var2;
                }
            }

            return this;
        }

        private boolean d(double var1) {
            return this.amm <= this.amn?this.amm <= var1 && var1 <= this.amn:this.amm <= var1 || var1 <= this.amn;
        }

        public LatLngBounds build() {
            jx.a(!Double.isNaN(this.amm), "no included points");
            return new LatLngBounds(new LatLng(this.amk, this.amm), new LatLng(this.aml, this.amn));
        }
    }
